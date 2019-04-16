package toy

import io.circe._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.circe._
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.middleware.FollowRedirect
import scala.concurrent.ExecutionContext.global
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

object Main extends IOApp with Http4sClientDsl[IO] {
  final case class Recipe(href: String, title: String)
  object Recipe {
    implicit val recipeDecoder: Decoder[Recipe] = new Decoder[Recipe] {
      def apply(c: HCursor): Decoder.Result[Recipe] =
        (c.downField("href").as[String], c.downField("title").as[String])
          .mapN(Recipe.apply)
    }
  }

  private final case class RecipePage(recipes: List[Recipe]) extends AnyVal
  private object RecipePage {
    implicit val recipePageDecoder: Decoder[RecipePage] =
      new Decoder[RecipePage] {
        def apply(c: HCursor): Decoder.Result[RecipePage] =
          c.downField("results")
            .as[List[Recipe]]
            .map(RecipePage.apply)
      }
    implicit val recipePageEntityDecoder: EntityDecoder[IO, RecipePage] = jsonOf
  }

  val baseUri = Uri.uri("http://www.recipepuppy.com/api/")

  def spider(
      C: Client[IO],
      L: Logger[IO]
  )(dish: String, ingredient: String, page: Int = 0): IO[Unit] = {
    val pageUri = baseUri
      .withQueryParam("q", dish)
      .withQueryParam("i", ingredient)
      .withQueryParam("p", page)
    (C.expect[RecipePage](GET(pageUri)) <* L.info(s"loaded page ${page}"))
      .map(_.recipes)
      .flatMap(
        recipes =>
          if (recipes.isEmpty) L.info("finished crawling")
          else
            recipes.traverse_(checkRecipe(C, L)) >> spider(C, L)(
              dish,
              ingredient,
              page + 1
            )
      )
      .recoverWith({
        case t: Throwable =>
          L.warn(s"failed to load page ${page}: ${t.getMessage}") >> spider(
            C,
            L
          )(dish, ingredient, page + 1)
      })
  }

  def checkRecipe(C: Client[IO], L: Logger[IO])(recipe: Recipe): IO[Unit] = {
    val check =
      (Uri
        .fromString(recipe.href)
        .liftTo[IO]
        .flatMap(uri => FollowRedirect(10)(C).expect[String](GET(uri))) >>
        L.info(s"valid recipe ${recipe.title} at ${recipe.href}"))
        .recoverWith({
          case t: Throwable =>
            L.warn(s"failed to access recipe ${recipe.title} at ${recipe.href}: ${t}")
        })

    L.info(s"checking recipe: ${recipe.title} at ${recipe.href}") >> check.void
  }

  def run(args: List[String]) =
    AsyncHttpClient.resource[IO]()
    // BlazeClientBuilder[IO](global).resource
      .use(
        client =>
          Slf4jLogger
            .create[IO]
            .flatMap(
              logger =>
                spider(client, logger)(args(0), args(1)).as(ExitCode.Success)
            )
      )
}
