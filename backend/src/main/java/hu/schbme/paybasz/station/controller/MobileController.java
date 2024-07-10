package hu.schbme.paybasz.station.controller;

import hu.schbme.paybasz.station.config.AppUtil;
import hu.schbme.paybasz.station.dto.*;
import hu.schbme.paybasz.station.error.UnauthorizedGateway;
import hu.schbme.paybasz.station.mapper.AccountMapper;
import hu.schbme.paybasz.station.mapper.ItemMapper;
import hu.schbme.paybasz.station.model.AccountEntity;
import hu.schbme.paybasz.station.repo.AccountRepository;
import hu.schbme.paybasz.station.service.GatewayService;
import hu.schbme.paybasz.station.service.LoggingService;
import hu.schbme.paybasz.station.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static hu.schbme.paybasz.station.PaybaszApplication.VERSION;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
@RequiredArgsConstructor
public class MobileController {

	private final TransactionService system;
	private final GatewayService gateways;
	private final LoggingService logger;
	private final AccountRepository accounts;

	@PostMapping("/app")
	public ResponseEntity<AppResponse> app(@RequestBody AuthorizedApiRequest request) {
		final boolean isUploader = gateways.authorizeUploaderGateway(request.getGatewayName(), request.getGatewayCode());
		if (!isUploader && !gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			final var items = system.getAllActiveItems()
					.stream()
					.map(ItemMapper.INSTANCE::toView)
					.toList();
			final var response = AppResponse.builder()
					.isUploader(isUploader)
					.items(items)
					.build();
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error creating app response", e);
			logger.failure("Sikertelen app információ lekérés: belső szerver hiba");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/upload/{gatewayName}")
	public PaymentStatus upload(@PathVariable String gatewayName, @RequestBody PaymentRequest request) {
		if (!gateways.authorizeUploaderGateway(gatewayName, request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());
		if (request.getAmount() < 0)
			return PaymentStatus.INTERNAL_ERROR;

		try {
			return system.addMoneyToCard(request.getCard().toUpperCase(), request.getAmount(),
					request.getDetails() == null ? "" : request.getDetails(),
					request.getGatewayName());
		} catch (Exception e) {
			log.error("Error during proceeding payment", e);
			logger.failure("Sikertelen fizetés: belső szerver hiba");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@PostMapping("/free-beer")
	public PaymentStatus freeBeer(@RequestBody PaymentRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());

		try {
			return system.getBeer(request.getCard().toUpperCase(),
					request.getDetails() == null ? "" : request.getDetails(),
					request.getGatewayName());
		} catch (Exception e) {
			log.error("Error during proceeding free beer", e);
			logger.failure("Sikertelen ingyen sör: belső szerver hiba");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@PostMapping("/pay")
	public PaymentStatus pay(@RequestBody PaymentRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());
		if (request.getAmount() < 0)
			return PaymentStatus.INTERNAL_ERROR;

		try {
			return system.proceedPayment(request.getCard().toUpperCase(), request.getAmount(),
					request.getDetails() == null ? "" : request.getDetails(),
					request.getGatewayName());
		} catch (Exception e) {
			log.error("Error during proceeding payment", e);
			logger.failure("Sikertelen fizetés: belső szerver hiba (terminál: " + request.getGatewayName() + ")");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@PostMapping("/buy-item")
	public PaymentStatus buyItem(@RequestBody ItemPurchaseRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());
		try {
			return system.decreaseItemCountAndBuy(request.getCard().toUpperCase(), request.getGatewayName(), request.getId());
		} catch (Exception e) {
			logger.failure("Sikertelen termék vásárlása: " + request.getId());
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	/**
	 * NOTE: Do not use for transaction purposes. Might be effected by dirty read.
	 */
	@PostMapping("/balance")
	public ResponseEntity<BalanceResponse> balance(@RequestBody BalanceRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		gateways.updateLastUsed(request.getGatewayName());
		log.info("New balance from gateway '{}' card hash: '{}'", request.getGatewayName(), request.getCard().toUpperCase());
		Optional<AccountEntity> account = system.getAccountByCard(request.getCard().toUpperCase());
		if (account.isEmpty()) {
			logger.action("<color>Ismeretlen kártya került leolvasásra.</color> (terminál: " + request.getGatewayName() + ")");
			return ResponseEntity.notFound().build();
		}

		var accountBalance = account.get();
		logger.action("<badge>" + account.map(AccountEntity::getName).orElse("n/a")
				+ "</badge> egyenlege leolvasva: <color>" + accountBalance.getBalance() + " JMF</color> (terminál: "
				+ request.getGatewayName() + ")");

		var response = BalanceResponse.builder()
				.balance(accountBalance.getBalance())
				.maxLoan(accountBalance.getMaxLoan())
				.build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/reading")
	public ValidationStatus reading(@RequestBody ReadingRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return ValidationStatus.INVALID;

		log.info("New reading from gateway '{}' read card hash: '{}'", request.getGatewayName(),
				request.getCard().toUpperCase());
		logger.action("Leolvasás történt: <badge>" + request.getCard().toUpperCase() + "</badge> (terminál: "
				+ request.getGatewayName() + ")");
		gateways.appendReading(request.getGatewayName(), request.getCard().toUpperCase());
		gateways.updateLastUsed(request.getGatewayName());
		return ValidationStatus.OK;
	}

	@PostMapping("/query")
	public ItemQueryResult query(@RequestBody ItemQueryRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return new ItemQueryResult(false, "unauthorized", 0);
		gateways.updateLastUsed(request.getGatewayName());

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
				+ "by Balázs;" // If you fork it, include your name
				+ "Time:;"
				+ AppUtil.DATE_ONLY_FORMATTER.format(System.currentTimeMillis()) + ";"
				+ AppUtil.TIME_ONLY_FORMATTER.format(System.currentTimeMillis());
	}

	@PostMapping("/set-card")
	public ResponseEntity<AccountEntity> addCard(@RequestBody AddCardRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		gateways.updateLastUsed(request.getGatewayName());

		if (accounts.findByCard(request.getCard().toUpperCase()).isPresent())
			return ResponseEntity.status(HttpStatus.CONFLICT).build();// card is already assigned

		Optional<AccountEntity> user = accounts.findById(request.getUserId());
		if (user.isEmpty())
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		final var account = user.get();
		if (!account.getCard().isEmpty())
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // User already has a card assigned

		account.setCard(request.getCard().toUpperCase());
		log.info("New card assignment from gateway '{}' card hash: '{}', user: {}", request.getGatewayName(), request.getCard(),
				account.getName());
		logger.action("<color>" + account.getName() + "</color> felhasználóhoz kártya rendelve: <badge>"
				+ request.getCard() + "</badge>  (terminál: " + request.getGatewayName() + ")");
		accounts.save(account);
		return ResponseEntity.ok(account);
	}

	@PostMapping("/get-user")
	public String getUser(@RequestBody GetUserRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			throw new UnauthorizedGateway();

		Optional<AccountEntity> account = accounts.findById(request.getUserId());
		if (account.isEmpty())
			return "USER_NOT_FOUND";

		logger.note("<color>" + account.get().getName() + "</color> felhasználó nevének lekérdezés (terminál: "
				+ request.getGatewayName() + ")");
		return account.get().getName();
	}

}
