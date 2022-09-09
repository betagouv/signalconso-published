package repositories.event

import utils.Constants.ActionEvent.ActionEventValue
import utils.Constants.EventType.EventTypeValue

case class EventFilter(eventType: Option[EventTypeValue] = None, action: Option[ActionEventValue] = None)
