package com.dichotome.etl.loader

import com.dichotome.etl.dao.DimensionsDao
import com.dichotome.etl.dao.FactDao
import com.dichotome.etl.dao.impl.PostgresDaoFactory
import com.dichotome.etl.data.mappers.DataParser
import com.dichotome.etl.forEachParallel
import com.dichotome.etl.source.Source
import com.dichotome.etl.tables.Fact
import com.dichotome.etl.utils.DimensionsStage
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

object EtlLoader {
    private val dimensionsDao: DimensionsDao = PostgresDaoFactory.createDimensionDao()
    private val factDao: FactDao = PostgresDaoFactory.createFactDao()

    fun load() {
        println("Loading commenced")
        println("Setting up the stage area")
        setUpStage()

        println("Uploading facts to the database")
        setUpFacts(DimensionsStage.facts)

        println("Loaded successfully")
    }

    private fun setUpStage() {
        with(DimensionsStage) {
            println("Parsing CSV files")

            facts.addAll(
                DataParser.parse(
                    Source.DiseasesCases,
                    Source.AnimalsInfo,
                    Source.HumanVictimsInfo,
                    Source.DiseaseInfo
                )
            )

            println("Finished parsing CSV")

            measureTimeMillis {
                runBlocking {
                    locationDims.forEachParallel { it.id = dimensionsDao.createLocationDim(it) }
                    observationDateDims.forEachParallel { it.id = dimensionsDao.createObservationDateDim(it) }
                    reportingDateDims.forEachParallel { it.id = dimensionsDao.createReportingDateDim(it) }
                    statusDims.forEachParallel { it.id = dimensionsDao.createStatusDim(it) }
                    diseaseDims.forEachParallel { it.id = dimensionsDao.createDiseaseDim(it) }
                    serotypeDims.forEachParallel { it.id = dimensionsDao.createSerotypeDim(it) }
                    speciesDims.forEachParallel { it.id = dimensionsDao.createSpeciesDim(it) }
                    sourceDims.forEachParallel { it.id = dimensionsDao.createSourceDim(it) }
                }
            }.also {
                println("Execution time $it ms")
            }
        }
    }

    private fun setUpFacts(facts: List<Fact>) {
        factDao.createAllFacts(facts)
    }
}