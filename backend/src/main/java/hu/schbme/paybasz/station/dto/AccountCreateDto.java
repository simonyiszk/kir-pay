package hu.schbme.paybasz.station.dto;

import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreateDto {

	private Integer id = null;
	private String name;
	private String email;
	private String phone;
	private String card;
	private Integer loan = 0;
	private String comment;

	@Transient
	public int getMaxLoan() {
		return loan;
	}
}
