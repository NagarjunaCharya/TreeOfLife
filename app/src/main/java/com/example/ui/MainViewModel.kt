package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Plant
import com.example.data.PlantRepository
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.GoogleSearchConfig
import com.example.network.InlineData
import com.example.network.MoshiTool
import com.example.network.Part
import com.example.network.RetrofitClient
import com.example.network.SpeechConfig
import com.example.network.VoiceConfig
import com.example.network.PrebuiltVoiceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.BuildConfig
import com.example.network.ThinkingConfig

import kotlinx.coroutines.flow.first

sealed class MainState {
    object Idle : MainState()
    object Loading : MainState()
    data class Success(val plants: List<Plant>) : MainState()
    data class Error(val message: String) : MainState()
}

class MainViewModel(
    private val repository: PlantRepository,
    private val sharedPrefs: android.content.SharedPreferences
) : ViewModel() {
    private val _state = MutableStateFlow<MainState>(MainState.Idle)
    val state: StateFlow<MainState> = _state.asStateFlow()

    var apiKey: String = sharedPrefs.getString("API_KEY", BuildConfig.GEMINI_API_KEY) ?: BuildConfig.GEMINI_API_KEY
        private set

    fun updateApiKey(newKey: String) {
        apiKey = newKey
        sharedPrefs.edit().putString("API_KEY", newKey).apply()
    }
    val plants = repository.allPlants

    // Chatbot state
    private val _chatMessages = MutableStateFlow<List<Content>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()
    
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    init {
        // Provide system instructions for chatbot
        _chatMessages.value = listOf(
            Content(role = "system", parts = listOf(Part(text = "You are a plant care expert and agricultural assistant. Answer user queries about plant health, care tips, or recommendations short and concisely.")))
        )
    }

    fun deletePlant(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun analyzePlantImage(imageBase64: String, plantName: String, onComplete: (Plant) -> Unit) {
        _state.value = MainState.Loading
        viewModelScope.launch {
            try {
                // Using 3.1-pro-preview with HIGH thinking for difficult task
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(
                            Part(text = "You are an expert plant pathologist. Identify the plant and any potential diseases or health issues from the image. Provide a detailed report including:\n1. Diagnosis\n2. Explanation of the disease\n3. Why it occurs\n4. Precautions\n5. Remedies"),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64))
                        ))
                    )
                )

                val response = RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Analysis incomplete."
                
                // Parse a simple short diagnosis and the rest as recommendation
                val diagnosis = text.take(60) + "..." 
                val plant = Plant(
                    name = plantName,
                    imageUrl = "", // simplified
                    diagnosis = diagnosis,
                    recommendation = text
                )
                repository.insert(plant)
                _state.value = MainState.Idle
                onComplete(plant)
            } catch (e: Exception) {
                _state.value = MainState.Error("Failed to analyze: ${e.message}")
            }
        }
    }

    fun generateSpeechTask(text: String, onAudioReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = text)))),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO"),
                        speechConfig = SpeechConfig(
                            VoiceConfig(PrebuiltVoiceConfig("Puck"))
                        )
                    )
                )
                val response = RetrofitClient.service.generateContent("gemini-2.5-flash-preview-tts", apiKey, request)
                val base64Audio = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.inlineData?.data
                if (base64Audio != null) {
                    onAudioReady(base64Audio)
                }
            } catch (e: Exception) {
                // Handle error siliently or log
            }
        }
    }

    fun exportCsvData(context: android.content.Context) {
        viewModelScope.launch {
            val list = plants.first()
            val csv = StringBuilder("ID,Name,Diagnosis,Recommendation\n")
            list.forEach { 
                csv.append("${it.id},\"${it.name}\",\"${it.diagnosis}\",\"${it.recommendation.replace("\"", "\"\"")}\"\n")
            }
            try {
                val file = java.io.File(context.getExternalFilesDir(null), "health_history.csv")
                file.writeText(csv.toString())
                // In a real app we would share this using FileProvider. For this prototype, saving to external files dir is sufficient.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessageToChat(message: String) {
        val userContent = Content(role = "user", parts = listOf(Part(text = message)))
        _chatMessages.value = _chatMessages.value + userContent
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val request = GenerateContentRequest(
                    // filter out system prompt manually or format correctly
                    contents = _chatMessages.value.filter { it.role != "system" },
                    systemInstruction = _chatMessages.value.firstOrNull { it.role == "system" }
                )
                val response = RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Sorry, I could not respond."
                val modelContent = Content(role = "model", parts = listOf(Part(text = replyText)))
                _chatMessages.value = _chatMessages.value + modelContent
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + Content(role = "model", parts = listOf(Part(text = "Error: ${e.message}")))
            } finally {
                _isChatLoading.value = false
            }
        }
    }
}
