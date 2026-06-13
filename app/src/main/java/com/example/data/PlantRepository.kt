package com.example.data

import kotlinx.coroutines.flow.Flow

class PlantRepository(private val plantDao: PlantDao) {
    val allPlants: Flow<List<Plant>> = plantDao.getAllPlants()

    suspend fun insert(plant: Plant) = plantDao.insertPlant(plant)

    suspend fun deleteById(id: Int) = plantDao.deletePlantById(id)
}
