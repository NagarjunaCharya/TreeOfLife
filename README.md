# TreeOfLife (PlantDoc)

TreeOfLife is an AI-powered Android application designed to help users identify and care for their plants. Utilizing the Google Gemini API, it provides intelligent insights into plant health, care instructions, and detailed plant information.

## Features

- **AI Plant Analysis**: Leverage the power of the Gemini API to analyze plants and receive intelligent care suggestions and information.
- **Local Plant Database**: Save your plants locally using a Room Database, allowing you to keep track of your personal garden.
- **Modern UI**: Built entirely with Jetpack Compose for a sleek, reactive, and modern user experience.
- **Local Storage**: Uses SharedPreferences and Room Database to keep user preferences and plant data secure and accessible offline.

## Tech Stack

- **Kotlin**
- **Jetpack Compose** (UI)
- **Room Database** (Local Storage)
- **ViewModel** (State Management)
- **Gemini API** (AI Integration)

## Setup Instructions

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Latest Version)
- A valid [Google Gemini API Key](https://aistudio.google.com/)

### Running Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/NagarjunaCharya/TreeOfLife.git
   ```
2. **Open the project in Android Studio.**
3. **Configure your API Key:**
   - Create a file named `.env` in the root project directory.
   - Add your Gemini API key in the file:
     ```
     GEMINI_API_KEY=your_api_key_here
     ```
   - *(See `.env.example` for reference)*.
4. **Sync Gradle:** Allow Android Studio to download dependencies and sync the project.
5. **Run the App:** Select your emulator or physical device and click **Run**.

## License

This project is open-source and available under the [MIT License](LICENSE).
