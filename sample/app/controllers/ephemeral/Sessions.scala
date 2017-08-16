package controllers.ephemeral

import javax.inject.Inject

import jp.t2v.lab.play2.auth.LoginLogout
import jp.t2v.lab.play2.auth.sample.Accounts
import play.api.Environment
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.crypto.CookieSigner
import play.api.mvc.InjectedController
import views.html

import scala.concurrent.{ExecutionContext, Future}

class Sessions @Inject()(val environment: Environment, val accounts: Accounts, val signer: CookieSigner)(implicit val executionContext: ExecutionContext) extends InjectedController with LoginLogout with AuthConfigImpl {

  val loginForm = Form {
    mapping("email" -> email, "password" -> text)(accounts.authenticate)(_.map(u => (u.email, "")))
      .verifying("Invalid email or password", result => result.isDefined)
  }

  def login = Action { implicit request =>
    Ok(html.ephemeral.login(loginForm))
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded.map(_.flashing(
      "success" -> "You've been logged out"
    ))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(html.ephemeral.login(formWithErrors))),
      user => gotoLoginSucceeded(user.get.id)
    )
  }

}