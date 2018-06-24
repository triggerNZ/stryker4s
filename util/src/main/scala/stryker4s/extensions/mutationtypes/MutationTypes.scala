package stryker4s.extensions.mutationtypes

import scala.meta.contrib._
import scala.meta.{Lit, Term, Tree}

/** Base trait for mutations. Mutations can be used to pattern match on (see MutantMatcher). <br>
  * Can also implicitly be converted to the appropriate [[scala.meta.Tree]] by importing [[stryker4s.extensions.ImplicitMutationConversion]]
  *
  * @tparam T Has to be a subtype of Tree.
  *           This is so that the tree value and unapply methods return the appropriate type.
  *           E.G. A False is of type [[scala.meta.Lit.Boolean]] instead of a standard [[scala.meta.Term]]
  */
trait Mutation[T <: Tree] {
  val tree: T

  def unapply(arg: T): Option[T] = Some(arg).filter(a => a.isEqual(tree))
}

trait TermNameMutation extends Mutation[Term.Name]

trait LiteralMutation[T <: Lit] extends Mutation[T]