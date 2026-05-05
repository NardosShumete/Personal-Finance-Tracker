package com.portfolio.financetracker.data.local.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.portfolio.financetracker.data.local.dao.TransactionDao
import com.portfolio.financetracker.data.local.entity.TransactionEntity
import com.portfolio.financetracker.data.mapper.toDomainModel
import com.portfolio.financetracker.domain.model.Transaction

/**
 * PagingSource that bridges Room → Paging 3 → Domain model.
 *
 * Room already generates a [PagingSource<Int, TransactionEntity>] via the DAO.
 * This class wraps it to perform the entity → domain mapping so the domain
 * layer stays free of Room types.
 *
 * Key = Int  (page/offset key managed by Room)
 * Value = Transaction (domain model, not the Room entity)
 */
class TransactionPagingSource(
    private val dao: TransactionDao
) : PagingSource<Int, Transaction>() {

    // Delegate to Room's generated PagingSource for the actual DB reads.
    private val roomSource: PagingSource<Int, TransactionEntity> =
        dao.getTransactionsPaged()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        return when (val result = roomSource.load(params)) {
            is LoadResult.Page -> LoadResult.Page(
                data       = result.data.map { it.toDomainModel() },
                prevKey    = result.prevKey,
                nextKey    = result.nextKey,
                itemsBefore = result.itemsBefore,
                itemsAfter  = result.itemsAfter
            )
            is LoadResult.Error -> LoadResult.Error(result.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? {
        // Map the anchor position back through the Room source
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    // Invalidate this source when the underlying Room source is invalidated
    // (e.g. after an insert or delete).
    override val jumpingSupported: Boolean get() = roomSource.jumpingSupported

    init {
        roomSource.registerInvalidatedCallback { invalidate() }
    }
}
