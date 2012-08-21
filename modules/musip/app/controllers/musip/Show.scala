package controllers.musip

import controllers.{Breadcrumbs, DelvingController}
import core.rendering.ViewRenderer
import play.api.i18n.Lang
import play.api.mvc.{RequestHeader, Action}
import models.{DomainConfiguration, MetadataCache}
import core.Constants._
import com.mongodb.BasicDBList
import com.mongodb.casbah.Imports._
import scala.Some

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */

object Show extends DelvingController {

  def museumViewDefinition(configuration: DomainConfiguration) = ViewRenderer.fromDefinition("musip", "museum")(configuration)

  def collectionViewDefinition(configuration: DomainConfiguration) = ViewRenderer.fromDefinition("musip", "collection")(configuration)


  def collection(orgId: String, itemId: String) = DomainConfigured {
    Root {
      Action {
        Action {
          implicit request => show(orgId, itemId, "collection", collectionViewDefinition(configuration)) match {
            case Some((viewTree, systemFields: Map[String, Map[String, String]], returnToResults: String, searchTerm: String)) =>

              val fields = systemFields.get("delving_title").getOrElse(new BasicDBList).asInstanceOf[BasicDBList]

              renderArgs += ("breadcrumbs" -> Breadcrumbs.crumble(
                Map(
                  "search" -> Map("searchTerm" -> searchTerm, "returnToResults" -> returnToResults),
                  "title" -> Map("url" -> "", "label" -> Option(fields.get(0)).getOrElse("").toString),
                  "inOrg" -> Map("inOrg" -> "yes")
                )
              ))

              Ok(Template("show.html", 'view -> viewTree, 'systemFields -> systemFields))
            case None => NotFound("Collection not found")
          }
        }
      }
    }
  }

  def museum(orgId: String, itemId: String) = DomainConfigured {
    Root {
      Action {
        implicit request => show(orgId, itemId, "museum", museumViewDefinition(configuration)) match {
          case Some((viewTree, systemFields: Map[String, Map[String, String]], returnToResults: String, searchTerm: String)) =>
            val fields = systemFields.get("delving_title").getOrElse(new BasicDBList).asInstanceOf[BasicDBList]

            renderArgs += ("breadcrumbs" -> Breadcrumbs.crumble(
              Map(
                "search" -> Map("searchTerm" -> searchTerm, "returnToResults" -> returnToResults),
                "title" -> Map("url" -> "", "label" -> Option(fields.get(0)).getOrElse("").toString),
                "inOrg" -> Map("inOrg" -> "yes")
              )
            ))
            
            Ok(Template("show.html", 'view -> viewTree, 'systemFields -> systemFields))
          case None => NotFound("Museum not found")
        }
      }
    }
  }

  private def show(orgId: String, itemId: String, itemType: String, renderer: Option[ViewRenderer])(implicit request: RequestHeader) = {
    val cache = MetadataCache.get(orgId, "musip", itemType)
    cache.findOne(itemId).map {
      thing =>
        if (!renderer.isDefined) {
          InternalServerError("Could not find renderer for " + itemType)
        } else {

          val navigateFromSearch = request.headers.get(REFERER) != None && request.headers.get(REFERER).get.contains("search")
          val navigateFromRelatedItem = request.queryString.getFirst("mlt").getOrElse("false").toBoolean
          val updatedSession = if (!navigateFromSearch && !navigateFromRelatedItem) {
            // we're coming from someplace else then a search, remove the return to results cookie
            request.session - (RETURN_TO_RESULTS)
          } else {
            request.session
          }

          val returnToResults = updatedSession.get(RETURN_TO_RESULTS).getOrElse("")
          val searchTerm = updatedSession.get(SEARCH_TERM).getOrElse("")


          val renderResult = renderer.get.renderRecord(thing.xml("musip"), getUserGrantTypes(orgId), Map("musip" -> "http://www.musip.nl/"), Lang(getLang), Map("orgId" -> orgId))
          val viewTree = renderResult.toViewTree

          (viewTree, thing.systemFields, returnToResults, searchTerm)
        }
    }
  }


}
