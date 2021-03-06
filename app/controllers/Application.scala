package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import views._
import play.api.libs.json.JsValue

object Application extends Controller {

  // -- Actions

  /**
   * Home page
   */
  def index = Action {
    Ok(html.index("Android String tool", null)(null))
  }


  // -- Javascript routing

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        Processing.transform
      )
    ).as("text/javascript")
  }


}
