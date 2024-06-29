package hu.schbme.paybasz.station.controller;

import hu.schbme.paybasz.station.config.AppUtil;
import hu.schbme.paybasz.station.dto.*;
import hu.schbme.paybasz.station.error.UnauthorizedGateway;
import hu.schbme.paybasz.station.model.AccountEntity;
import hu.schbme.paybasz.station.model.ItemEntity;
import hu.schbme.paybasz.station.service.GatewayService;
import hu.schbme.paybasz.station.service.LoggingService;
import hu.schbme.paybasz.station.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static hu.schbme.paybasz.station.PaybaszApplication.VERSION;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

	private final TransactionService system;
	private final GatewayService gateways;
	private final LoggingService logger;

	@PostMapping("/pay/{gatewayName}")
	public PaymentStatus pay(@PathVariable String gatewayName, @RequestBody PaymentRequest request) {
		if (!gateways.authorizeGateway(gatewayName, request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(gatewayName);
		if (request.getAmount() < 0)
			return PaymentStatus.INTERNAL_ERROR;

		try {
			return system.proceedPayment(request.getCard().toUpperCase(), request.getAmount(),
					request.getDetails() == null ? "" : request.getDetails(),
					gatewayName);
		} catch (Exception e) {
			log.error("Error during proceeding payment", e);
			logger.failure("Sikertelen fizetés: belső szerver hiba");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	/**
	 * NOTE: Do not use for transaction purposes. Might be effected by dirty read.
	 */
	@PutMapping("/balance/{gatewayName}")
	public AccountBalance balance(@PathVariable String gatewayName, @RequestBody BalanceRequest request) {
		if (!gateways.authorizeGateway(gatewayName, request.getGatewayCode()))
			throw new UnauthorizedGateway();

		gateways.updateLastUsed(gatewayName);
		log.info("New balance from gateway '{}' card hash: '{}'", gatewayName, request.getCard().toUpperCase());
		Optional<AccountEntity> account = system.getAccountByCard(request.getCard().toUpperCase());
		var accountBalance = account.map(accountEntity -> new AccountBalance(accountEntity.getBalance(), isLoadAllowed(accountEntity), accountEntity.isAllowed()))
				.orElseGet(() -> new AccountBalance(0, false, false));

		logger.action("<badge>" + account.map(AccountEntity::getName).orElse("n/a")
				+ "</badge> egyenlege leolvasva: <color>" + accountBalance.getBalance() + " JMF</color>");
		return accountBalance;
	}

	@PutMapping("/validate/{gatewayName}")
	public ValidationStatus validate(@PathVariable String gatewayName, @RequestBody ValidateRequest request) {
		boolean valid = gateways.authorizeGateway(gatewayName, request.getGatewayCode());
		log.info("Gateways auth request: {} ({})", gatewayName, valid ? "OK" : "INVALID");
		if (valid) {
			gateways.updateLastUsed(gatewayName);
			logger.action("Terminál authentikáció sikeres: <color>" + gatewayName + "</color>");
		} else {
			logger.failure("Terminál authentikáció sikertelen: <color>" + gatewayName + "</color>");
		}
		return valid ? ValidationStatus.OK : ValidationStatus.INVALID;
	}

	@PutMapping("/reading/{gatewayName}")
	public ValidationStatus reading(@PathVariable String gatewayName, @RequestBody ReadingRequest readingRequest) {
		if (!gateways.authorizeGateway(gatewayName, readingRequest.getGatewayCode()))
			return ValidationStatus.INVALID;

		log.info("New reading from gateway '{}' read card hash: '{}'", gatewayName, readingRequest.getCard().toUpperCase());
		logger.action("Leolvasás történt: <badge>" + readingRequest.getCard().toUpperCase() + "</badge> (terminál: " + gatewayName + ")");
		gateways.appendReading(gatewayName, readingRequest.getCard().toUpperCase());
		gateways.updateLastUsed(gatewayName);
		return ValidationStatus.OK;
	}

	@PostMapping("/query/{gatewayName}")
	public ItemQueryResult query(@PathVariable String gatewayName, @RequestBody ItemQueryRequest request) {
		if (!gateways.authorizeGateway(gatewayName, request.getGatewayCode()))
			return new ItemQueryResult(false, "unauthorized", 0);
		gateways.updateLastUsed(gatewayName);

		try {
			return system.resolveItemQuery(request.getQuery());
		} catch (Exception e) {
			logger.failure("Sikertelen termék lekérdezés: " + request.getQuery());
			return new ItemQueryResult(false, "invalid query", 0);
		}
	}

	@GetMapping("/status")
	public String test(HttpServletRequest request) {
		log.info("Status endpoint triggered from IP: {}", request.getRemoteAddr());
		logger.serverInfo("Státusz olvasás a <color>" + request.getRemoteAddr() + "</color> címről");
		return "Server: " + VERSION + ";"
				+ "by Schami;" // If you fork it, include your name
				+ "Time:;"
				+ AppUtil.DATE_ONLY_FORMATTER.format(System.currentTimeMillis()) + ";"
				+ AppUtil.TIME_ONLY_FORMATTER.format(System.currentTimeMillis());
	}

	@GetMapping("/items/{gatewayName}")
	public List<ItemEntity> listItems(@PathVariable String gatewayName, @RequestBody ItemQueryRequest request) {
		if (!gateways.authorizeGateway(gatewayName, request.getGatewayCode()))
			return List.of();

		return system.getALlItems().stream()
				.filter(ItemEntity::isActive)
				.toList();
	}

	private boolean isLoadAllowed(AccountEntity accountEntity) {
		return accountEntity.getMinimumBalance() < 0;
	}

}
