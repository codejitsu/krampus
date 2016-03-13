// Copyright (C) 2016, codejitsu.

package controllers

import actors.RecipientActor
import com.typesafe.scalalogging.LazyLogging
import play.api.Play.current
import play.api.mvc.{Action, AnyContent, Controller, WebSocket}

object WikiwatchController extends Controller with LazyLogging {
  def stream(channel: String): WebSocket[String, String] = WebSocket.acceptWithActor[String, String] { request => out =>
    logger.debug("Try to connect to the wiki channel '{}'", channel)
    RecipientActor.props(out, s"#$channel.wikipedia")
  }

  def wikiwatch: Action[AnyContent] = Action { Ok(views.html.wikiwatch("WikiWatch")) }
}
