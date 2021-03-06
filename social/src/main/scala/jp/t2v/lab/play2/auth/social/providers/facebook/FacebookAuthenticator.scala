package jp.t2v.lab.play2.auth.social.providers.facebook

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import jp.t2v.lab.play2.auth.social.core.{AccessTokenRetrievalFailedException, OAuth2Authenticator}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.ws.{WSClient, WSResponse, _}
import play.api.{Configuration, Logger}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class FacebookAuthenticator @Inject()(val ws: WSClient, config: Configuration) extends OAuth2Authenticator {

  type AccessToken = String

  val providerName: String = "facebook"

  val accessTokenUrl = "https://graph.facebook.com/oauth/access_token"

  val authorizationUrl = "https://graph.facebook.com/oauth/authorize"

  lazy val clientId = config.get[String]("facebook.clientId")

  lazy val clientSecret = config.get[String]("facebook.clientSecret")

  lazy val callbackUrl = config.get[String]("facebook.callbackURL")

  def retrieveAccessToken(code: String)(implicit ctx: ExecutionContext): Future[AccessToken] = {
    ws.url(accessTokenUrl)
      .withQueryStringParameters(
        "client_id" -> clientId,
        "client_secret" -> clientSecret,
        "redirect_uri" -> callbackUrl,
        "code" -> code)
      .withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
      .post(EmptyBody)
      .map { response =>
        Logger(getClass).debug("Retrieving access token from provider API: " + response.body)
        parseAccessTokenResponse(response)
      }
  }

  def getAuthorizationUrl(scope: String, state: String): String = {
    val encodedClientId = URLEncoder.encode(clientId, "utf-8")
    val encodedRedirectUri = URLEncoder.encode(callbackUrl, "utf-8")
    val encodedScope = URLEncoder.encode(scope, "utf-8")
    val encodedState = URLEncoder.encode(state, "utf-8")
    s"${authorizationUrl}?client_id=${encodedClientId}&redirect_uri=${encodedRedirectUri}&scope=${encodedScope}&state=${encodedState}"
  }

  def parseAccessTokenResponse(response: WSResponse): String = {
    Logger(getClass).debug("Parsing access token response: " + response.body)
    try {
      (for {
        params <- response.body.split("&").toList
        key :: value :: Nil = params.split("=").toList
        if key == "access_token"
      } yield {
        value
      }).headOption.getOrElse {
        throw new AccessTokenRetrievalFailedException(s"Failed to parse access token: ${response.body}")
      }
    } catch {
      case NonFatal(e) =>
        throw new AccessTokenRetrievalFailedException(s"Failed to retrieve access token. ${response.body}", e)
    }

  }

}
