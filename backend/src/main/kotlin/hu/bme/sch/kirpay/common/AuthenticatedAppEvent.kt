package hu.bme.sch.kirpay.common

import hu.bme.sch.kirpay.principal.Principal


interface AuthenticatedAppEvent : AppEvent {

  val by: Principal?

}


interface AppEvent {

  val timestamp: Long

}
