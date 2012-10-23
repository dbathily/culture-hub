package controllers

import play.api.mvc._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.data.Forms._
import extensions.Extensions
import org.bson.types.ObjectId
import com.novus.salat
import salat.dao.SalatDAO
import play.api.data.FormError
import scala.Some

/**
 * Experimental CRUD controller.
 * The idea is to provide a number of generic methods handling the listing, submission (create or update), and deletion of a model.
 *
 * TODO look into a trait for the companion object of the ViewModel that would provide a Model => ViewModel transformation
 * TODO see how to handle the viewModel.copy(errors = ...) case.
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
trait CRUDController extends Logging with Extensions with RenderingExtensions { self: Controller with DomainConfigurationAware =>

  def handleSubmit[ViewModel <: CRUDViewModel, A <: salat.CaseClass, B <: SalatDAO[A, ObjectId]]
                  (form: Form[ViewModel], dao: B, update: (ViewModel, A) => Either[String, A], create: ViewModel => Either[String, (A, ViewModel)])
                  (implicit request: Request[AnyContent], mf: Manifest[A]): Result = {

    form.bind(request.body.asJson.get).fold(
      formWithErrors => handleValidationError(formWithErrors),
      boundViewModel => {
        boundViewModel.id match {
          case Some(id) =>
            dao.findOneById(id) match {
              case Some(existingModel) =>
                try {
                  update(boundViewModel, existingModel) match {
                    case Right(updatedModel) =>
                      info("Updated 's%' with identifier %s".format(mf.erasure.getName, id))
                      dao.save(updatedModel)
                      Json(existingModel)
                    case Left(errorMessage) =>
                      warning("Problem while updating '%s' with identifier %s: %s".format(mf.erasure.getName, id, errorMessage))
                      // TODO see if there's a way to abstract the copy method and return the full initial object.
                      Json(Map("errors" -> Map("global" -> errorMessage)))
                  }
                } catch {
                  case t: Throwable =>
                    logError(t, "Problem while updating '%s' with identifier %s".format(mf.erasure.getName, id))
                    // Json(boundForm.copy(errors = Map("global" -> t.getMessage)))
                    // TODO see if there's a way to abstract the copy method and return the full initial object.
                    Json(Map("errors" -> Map("global" -> t.getMessage)))
                }
              case None =>
                Error("Model of type '%s' was not found for identifier %s".format(mf.erasure.getName, id))
            }
          case None =>
            create(boundViewModel) match {
              case Right((createdModel, createdViewModel)) =>
                dao.insert(createdModel)
                Json(createdViewModel)
              case Left(errorMessage) =>
                Json(Map("errors" -> Map("global" -> errorMessage)))
            }
        }
      }
    )



  }

    // ~~~ form handling when using knockout. This returns a map of error messages

  def handleValidationError[T](form: Form[T])(implicit request: RequestHeader) = {
    val e: Seq[FormError] = form.errors
    val fieldErrors = e.filterNot(_.key.isEmpty).map(error => (error.key.replaceAll("\\.", "_"), Messages(error.message, error.args))).toMap
    val globalErrors = e.filter(_.key.isEmpty).map(error => ("global", Messages(error.message, error.args))).toMap
    Json(Map("errors" -> (fieldErrors ++ globalErrors)), BAD_REQUEST)
  }

  // ~~~ Form utilities
  import extensions.Formatters._

  val tokenListMapping = seq(
    play.api.data.Forms.mapping(
      "id" -> text,
      "name" -> text,
      "tokenType" -> optional(text),
      "data" -> optional(of[Map[String, String]])
      )(Token.apply)(Token.unapply)
    )

}

abstract class CRUDViewModel extends ViewModel {
  val id: Option[ObjectId]
}
