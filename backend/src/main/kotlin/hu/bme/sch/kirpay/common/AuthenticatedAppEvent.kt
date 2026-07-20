package hu.bme.sch.kirpay.common

import hu.bme.sch.kirpay.principal.PrincipalRef


interface AuthenticatedAppEvent : AppEvent {

  val by: PrincipalRef?

}


interface AppEvent {

  val timestamp: Long

}
