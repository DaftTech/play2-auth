package controllers.basic

import java.nio.charset.Charset
import java.util.Base64

import jp.t2v.lab.play2.auth.{AuthenticityToken, TokenAccessor}
import play.api.libs.crypto.CookieSigner
import play.api.mvc.{RequestHeader, Result}

class BasicAuthTokenAccessor extends TokenAccessor {

  override def delete(result: Result)(implicit request: RequestHeader): Result = result

  override def put(signer: CookieSigner, token: AuthenticityToken)(result: Result)(implicit request: RequestHeader):
  Result = result

  override def extract(signer: CookieSigner, request: RequestHeader): Option[AuthenticityToken] = {
    val encoded = for {
      h <- request.headers.get("Authorization")
      if h.startsWith("Basic ")
    } yield h.substring(6)
    encoded.map(s => new String(Base64.getDecoder.decode(s), Charset.forName("UTF-8")))
  }

}
