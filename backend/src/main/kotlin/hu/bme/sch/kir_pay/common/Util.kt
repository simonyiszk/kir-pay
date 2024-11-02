package hu.bme.sch.kir_pay.common

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity


fun ResponseEntity.BodyBuilder.asFileAttachment(filename: String) =
  header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
