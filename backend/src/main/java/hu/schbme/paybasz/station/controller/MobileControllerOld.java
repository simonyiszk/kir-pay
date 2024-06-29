package hu.schbme.paybasz.station.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
@Profile("beta")
@Controller
@RequestMapping("/mobile")
public class MobileControllerOld {

	@GetMapping("/")
	public String index() {
		return "mobile/mobile";
	}

	@GetMapping("/pay")
	public String pay() {
		return "mobile/pay";
	}

	@GetMapping("/read")
	public String read() {
		return "mobile/read";
	}

	@GetMapping("/scan")
	public String scan() {
		return "mobile/scan";
	}

}
