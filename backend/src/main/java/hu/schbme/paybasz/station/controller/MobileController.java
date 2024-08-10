package hu.schbme.paybasz.station.controller;

import hu.schbme.paybasz.station.config.AppUtil;
import hu.schbme.paybasz.station.config.MobileConfig;
import hu.schbme.paybasz.station.dto.*;
import hu.schbme.paybasz.station.error.UnauthorizedGateway;
import hu.schbme.paybasz.station.mapper.AccountMapper;
import hu.schbme.paybasz.station.mapper.ConfigMapper;
import hu.schbme.paybasz.station.mapper.ItemMapper;
import hu.schbme.paybasz.station.model.AccountEntity;
import hu.schbme.paybasz.station.model.ItemEntity;
import hu.schbme.paybasz.station.repo.AccountRepository;
import hu.schbme.paybasz.station.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static hu.schbme.paybasz.station.PaybaszApplication.VERSION;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
@RequiredArgsConstructor
public class MobileController {

	private final GatewayService gateways;
	private final LoggingService logger;
	private final AccountRepository accounts;
	private final MobileConfig mobileConfig;
	private final AccountService accountService;
	private final ItemService itemService;
	private final TransactionService transactionService;
	private final ItemTokenService itemTokenService;

	@Transactional
	@PostMapping("/app")
	public ResponseEntity<AppResponse> app(@RequestBody AuthorizedApiRequest request) {
		final boolean isUploader = gateways.authorizeUploaderGateway(request.getGatewayName(), request.getGatewayCode());
		if (!isUploader && !gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			final var items = itemService.getAllActiveItems()
					.stream()
					.map(ItemMapper.INSTANCE::toView)
					.toList();
			final var response = AppResponse.builder()
					.isUploader(isUploader)
					.canReassignCards(isUploader)
					.canTransferFunds(true)
					.items(items)
					.mobileConfig(ConfigMapper.INSTANCE.toView(mobileConfig))
					.build();
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Error creating app response", e);
			logger.failure("Sikertelen app információ lekérés: belső szerver hiba");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Transactional
	@PostMapping("/checkout")
	public PaymentStatus checkout(@Valid @RequestBody CheckoutRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode())) {
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		}

		try {
			return transactionService.checkout(request.getCard().toUpperCase(), request.getCart(), request.getGatewayName());
		} catch (Exception e) {
			log.error("Error creating app response", e);
			logger.failure("Sikertelen tranzakció: belső szerver hiba");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@Transactional
	@PostMapping("/users")
	public ResponseEntity<List<UserListItem>> getUsers(@RequestBody AuthorizedApiRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			var accounts = accountService.getAllAccounts();
			var userList = StreamSupport
					.stream(accounts.spliterator(), false)
					.map(AccountMapper.INSTANCE::toListItem)
					.toList();
			return ResponseEntity.ok(userList);
		} catch (Exception e) {
			log.error("Error returning all the users", e);
			logger.failure("A felhasználók lekérése sikertelen: belső szerver hiba");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

	}

	@Transactional
	@PostMapping("/upload")
	public PaymentStatus upload(@RequestBody PaymentRequest request) {
		if (!gateways.authorizeUploaderGateway(request.getGatewayName(), request.getGatewayCode())) {
			log.warn("Unauthorized gateway '{}' with token '{}'", request.getGatewayName(), request.getGatewayCode());
			logger.failure("Nem jogosult terminál: <color>" + request.getGatewayName() + "</color>");
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		}
		gateways.updateLastUsed(request.getGatewayName());
		if (request.getAmount() < 0)
			return PaymentStatus.INTERNAL_ERROR;

		try {
			return transactionService.addMoneyToCard(request.getCard().toUpperCase(), request.getAmount(),
					request.getDetails() == null ? "" : request.getDetails(),
					request.getGatewayName());
		} catch (Exception e) {
			log.error("Error during proceeding payment", e);
			logger.failure("Sikertelen fizetés: belső szerver hiba");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@Transactional
	@PostMapping("/pay")
	public PaymentStatus pay(@RequestBody PaymentRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());
		if (request.getAmount() < 0)
			return PaymentStatus.INTERNAL_ERROR;

		try {
			return transactionService.proceedPayment(request.getCard().toUpperCase(), request.getAmount(),
					request.getDetails() == null ? "" : request.getDetails(),
					request.getGatewayName());
		} catch (Exception e) {
			log.error("Error during proceeding payment", e);
			logger.failure("Sikertelen fizetés: belső szerver hiba (terminál: " + request.getGatewayName() + ")");
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@Transactional
	@PostMapping("/buy-item")
	public PaymentStatus buyItem(@RequestBody ItemPurchaseRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());
		try {
			return transactionService.decreaseItemCountAndBuy(request.getCard().toUpperCase(), request.getGatewayName(), request.getId());
		} catch (Exception e) {
			logger.failure("Sikertelen termék vásárlása: " + request.getId());
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@Transactional
	@PostMapping("/balance")
	public ResponseEntity<BalanceResponse> balance(@RequestBody BalanceRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		gateways.updateLastUsed(request.getGatewayName());
		log.info("New balance from gateway '{}' card hash: '{}'", request.getGatewayName(), request.getCard().toUpperCase());
		Optional<AccountEntity> account = accountService.getAccountByCard(request.getCard().toUpperCase());
		if (account.isEmpty()) {
			logger.action("<color>Ismeretlen kártya került leolvasásra.</color> (terminál: " + request.getGatewayName() + ")");
			return ResponseEntity.notFound().build();
		}

		var accountBalance = account.get();
		logger.action("<badge>" + account.map(AccountEntity::getName).orElse("n/a")
				+ "</badge> egyenlege leolvasva: <color>" + accountBalance.getBalance() + " JMF</color> (terminál: "
				+ request.getGatewayName() + ")");

		return ResponseEntity.ok(AccountMapper.INSTANCE.toBalance(accountBalance));
	}

	@Transactional
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

	@Transactional
	@PostMapping("/card-check")
	public ValidationStatus cardCheck(@RequestBody ReadingRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return ValidationStatus.INVALID;

		log.info("Card check from gateway '{}' card hash: '{}'", request.getGatewayName(),
				request.getCard().toUpperCase());
		logger.action("Kártya ellenőrzés történt: <badge>" + request.getCard().toUpperCase() + "</badge> (terminál: "
				+ request.getGatewayName() + ")");

		gateways.updateLastUsed(request.getGatewayName());
		if (accounts.findByCard(request.getCard().toUpperCase()).isPresent())
			return ValidationStatus.OK;

		return ValidationStatus.INVALID;
	}

	@Transactional
	@PostMapping("/claim-token")
	public PaymentStatus claimToken(@Valid @RequestBody ClaimTokenRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return PaymentStatus.UNAUTHORIZED_TERMINAL;
		gateways.updateLastUsed(request.getGatewayName());

		Optional<AccountEntity> account = accountService.getAccountByCard(request.getCard().toUpperCase());
		if (account.isEmpty()) {
			logger.action("<color>Ismeretlen kártya került leolvasásra.</color> (terminál: " + request.getGatewayName() + ")");
			return PaymentStatus.VALIDATION_ERROR;
		}

		Optional<ItemEntity> item = itemService.getItem(request.getItemId());
		if (item.isEmpty()) {
			logger.action("<color>Nem létező termék token</color> (terminál: " + request.getGatewayName() + ")");
			return PaymentStatus.VALIDATION_ERROR;
		}

		try {
			var result = itemTokenService.claimItemToken(account.get(), item.get(), 1);
			switch (result) {
				case SUCCESS -> {
					return PaymentStatus.ACCEPTED;
				}
				case NOT_ENOUGH_TOKENS -> {
					return PaymentStatus.NOT_ENOUGH_TOKENS;
				}
				case NO_TOKEN -> {
					return PaymentStatus.NOT_ENOUGH_CASH;
				}
				default -> {
					return PaymentStatus.VALIDATION_ERROR;
				}
			}
		} catch (Exception e) {
			logger.failure("Sikertelen token beváltása " + request.getItemId());
			return PaymentStatus.INTERNAL_ERROR;
		}
	}

	@Transactional
	@PostMapping("/query")
	public ItemQueryResult query(@RequestBody ItemQueryRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return new ItemQueryResult(false, "unauthorized", 0);
		gateways.updateLastUsed(request.getGatewayName());

		try {
			return itemService.resolveItemQuery(request.getQuery());
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

	@Transactional
	@PostMapping("/set-card")
	public ResponseEntity<AccountEntity> addCard(@RequestBody AddCardRequest request) {
		if (!gateways.authorizeGateway(request.getGatewayName(), request.getGatewayCode()))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		var canReassign = gateways.authorizeUploaderGateway(request.getGatewayName(), request.getGatewayCode());

		gateways.updateLastUsed(request.getGatewayName());

		var currentHolder = accounts.findByCard(request.getCard().toUpperCase());
		if (currentHolder.isPresent()) {
			if (!canReassign) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();// card is already assigned
			}
			currentHolder.get().setCard("");
		}

		Optional<AccountEntity> user = accounts.findById(request.getUserId());
		if (user.isEmpty())
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

		final var account = user.get();
		if (!account.getCard().isEmpty() && !canReassign)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // User already has a card assigned

		account.setCard(request.getCard().toUpperCase());
		log.info("New card assignment from gateway '{}' card hash: '{}', user: {}", request.getGatewayName(), request.getCard(),
				account.getName());
		logger.action("<color>" + account.getName() + "</color> felhasználóhoz kártya rendelve: <badge>"
				+ request.getCard() + "</badge>  (terminál: " + request.getGatewayName() + ")");
		accounts.save(account);
		if (currentHolder.isPresent() && !Objects.equals(currentHolder.get().getId(), account.getId())) {
			accounts.save(currentHolder.get());
		}
		return ResponseEntity.ok(account);
	}

	@Transactional
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
