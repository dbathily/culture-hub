package extensions

import play.classloading.enhancers.LocalvariablesNamesEnhancer
import play.mvc.Http.{Response, Request}
import net.liftweb.json.Serialization._
import java.lang.reflect.{Method, Constructor}
import play.templates.Html
import play.mvc.results.{RenderHtml, RenderXml, RenderJson, Result}
import models.User
import play.mvc.Before
import net.liftweb.json.{Xml, Extraction, DefaultFormats, ParameterNameReader}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

// glue for lift-json
object PlayParameterNameReader extends ParameterNameReader {
  def lookupParameterNames(constructor: Constructor[_]) = {
    Set(LocalvariablesNamesEnhancer.lookupParameterNames(constructor).toArray(new Array[String](1)): _*)
  }
}

/**
 * This trait provides additional actions that can be used in controllers
 */
trait AdditionalActions {

  def Json(data: AnyRef) = new RenderLiftJson(data)

  def RenderMultitype(template: play.templates.BaseScalaTemplate[play.templates.Html, play.templates.Format[play.templates.Html]], args: (Symbol, Any)*) = new RenderMultitype(template, args: _*)

  def RenderKml(entity: AnyRef) = new RenderKml(entity)

}

/**
 * Experimental action to generically render an object as json, xml or html depending on the requested format.
 */
class RenderMultitype(template: play.templates.BaseScalaTemplate[play.templates.Html, play.templates.Format[play.templates.Html]], args: (Symbol, Any)*) extends Result {

  def apply(request: Request, response: Response) {

    implicit val formats = new DefaultFormats {
      override val parameterNameReader = PlayParameterNameReader
    }

    // TODO we for the moment only handle a single parameter. We have to see how multi-parameter JSON and XML responses would look like
    val arg = args(0)._2.asInstanceOf[AnyRef]

    if (request.format == "json") {
      new RenderJson(write(arg))(request, response)
    } else if (request.format == "xml") {
      val doc = <response>
        {net.liftweb.json.Xml.toXml(Extraction.decompose(arg))}
      </response>
      new RenderXml(doc.toString())(request, response)
    } else if (request.format == "kml") {
      // TODO handle case when the entity does not support being rendered via KML
      new RenderKml(arg)(request, response)
    } else {
      // TODO this was hacked together in five minutes. Due to lack of knowledge of the scala reflection mechnism I resolved to the ugly code below
      // but I guess there is a better way
      val c = Class.forName(template.getClass.getName)
      val templateObject: AnyRef = c.getField("MODULE$").get(null).asInstanceOf[template.type]
      val method: Method = templateObject.getClass.getMethod("apply", arg.asInstanceOf[AnyRef].getClass)
      val invoked: Html = method.invoke(template, arg.asInstanceOf[AnyRef]).asInstanceOf[Html]

      new RenderHtml(invoked.toString())(request, response)
    }
  }

}


class RenderLiftJson(data: AnyRef) extends Result {
  def apply(request: Request, response: Response) {

    implicit val formats = new DefaultFormats {
      override val parameterNameReader = PlayParameterNameReader
    }

    new RenderJson(write(data))(request, response)
  }
}

class RenderKml(entity: AnyRef) extends Result {
  def apply(request: Request, response: Response) {
    val doc = entity match {
      case u: User => KMLSerializer.toKml(u)
      case _ => throw new RuntimeException("not implemented")
    }
    new RenderXml(doc.toString())(request, response)
  }
}
