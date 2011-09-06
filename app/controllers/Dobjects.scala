package controllers

import play.mvc.results.Result
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import models.{DObject, UserCollection}

/**
 *
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 * @author Sjoerd Siebinga <sjoerd@delving.eu>
 */

object DObjects extends DelvingController {

  import views.Dobject._

  def list(user: String): AnyRef = {
    val u = getUser(user)

    // TODO access rights
    val objects = Map("availableObjects" -> DObject.findByUser(browsedUserId).map {o => ObjectModel(Some(o._id), o.name, o.description, o.user_id)})

    request.format match {
      case "html" => html.list(user = u)
      case "json" => Json(objects)
    }
  }

  def view(user: String, id: String): AnyRef = {
    DObject.findById(id) match {
        case None => NotFound
        case Some(anObject) => html.dobject(dobject = anObject)
      }
  }

  def load(id: String): Result = {
    DObject.findById(id) match {
        case None => Json(ObjectModel.empty)
        case Some(anObject) => {
          val collections = ObjectModel.objectIdListToCollections(anObject.collections)
          Json(ObjectModel(Some(anObject._id), anObject.name, anObject.description, anObject.user_id, collections))
        }
      }
  }
}

case class ObjectModel(id: Option[ObjectId] = None, name: String = "", description: Option[String] = Some(""), owner: ObjectId, collections: List[Collection] = List.empty[Collection]) {
  def getCollections: List[ObjectId] = for(collection <- collections) yield new ObjectId(collection.id)
}

object ObjectModel {

  val empty: ObjectModel = ObjectModel(name = "", owner = new ObjectId())

  def objectIdListToCollections(collectionIds: List[ObjectId]) = {
    (for (userCollection: UserCollection <- UserCollection.find(MongoDBObject("_id" -> MongoDBObject("$in" -> collectionIds))))
    yield Collection(userCollection._id.toString, userCollection.name)).toList
  }

}

case class Collection(id: String, name: String)
