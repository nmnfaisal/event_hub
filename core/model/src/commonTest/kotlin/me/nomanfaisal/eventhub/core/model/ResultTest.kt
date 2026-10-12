package me.nomanfaisal.eventhub.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ResultTest {
    @Test
    fun mapTransformsSuccess() {
        val result: Result<Int, DataError> = Result.Success(2)
        assertEquals(Result.Success(4), result.map { it * 2 })
    }

    @Test
    fun mapLeavesErrorUntouched() {
        val result: Result<Int, DataError> = Result.Error(DataError.Network.NoInternet)
        assertEquals(Result.Error(DataError.Network.NoInternet), result.map { it * 2 })
    }

    @Test
    fun emptySuccessIsNotAnError() {
        val empty: Result<List<String>, DataError> = Result.Success(emptyList())
        val failed: Result<List<String>, DataError> = Result.Error(DataError.Network.NoInternet)
        assertNotEquals(empty, failed)
        assertTrue(empty is Result.Success)
    }

    @Test
    fun onSuccessAndOnErrorRunOnlyTheirBranch() {
        var successes = 0
        var errors = 0
        val ok: Result<Int, DataError> = Result.Success(1)
        val bad: Result<Int, DataError> = Result.Error(DataError.Network.Server)
        ok.onSuccess { successes++ }.onError { errors++ }
        bad.onSuccess { successes++ }.onError { errors++ }
        assertEquals(1, successes)
        assertEquals(1, errors)
    }
}
