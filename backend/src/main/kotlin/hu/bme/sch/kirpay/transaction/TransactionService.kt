package hu.bme.sch.kirpay.transaction

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class TransactionService(private val transactionRepository: TransactionRepository) {

  fun findAll() = transactionRepository.findAllOrderByTimestampDesc()


  fun findPaginated(page: Int, size: Int) =
    transactionRepository.findAllOrderByTimestampDescPaginated(page.toLong() * size, size)


  fun countAll() = transactionRepository.count()


  fun getIncome() = transactionRepository.getIncome()


  fun getTransactionVolume() = transactionRepository.getTransactionVolume()


  fun getAllUploads() = transactionRepository.getAllUploads()

}
