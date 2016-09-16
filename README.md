# Spring Data JPA Specification DSL for Kotlin [![Build Status](https://travis-ci.org/consoleau/kotlin-jpa-specification-dsl.svg?branch=master)](https://travis-ci.org/consoleau/kotlin-jpa-specification-dsl)

This library provides a fluent DSL for querying spring data JPA repositories using spring data Specifications (i.e. the JPA Criteria API), without boilerplate code or a generated metamodel.

# Example #

```
import com.console.specificationsdsl.*   // 1. Import Kotlin magic

////
// 2. Declare JPA Entities
@Entity
data class TvShow(
    @Id
    @GeneratedValue
    val id: Int = 0,
    val name: String = "",
    val synopsis: String = "",
    val availableOnNetflix: Boolean = false,
    val releaseDate: String? = null,
    @OneToMany(cascade = arrayOf(javax.persistence.CascadeType.ALL))
    val starRatings: Set<StarRating> = emptySet())

@Entity
data class StarRating(
    @Id
    @GeneratedValue
    val id: Int = 0,
    val stars: Int = 0)


////
// 3. Declare JPA Repository with JpaSpecificationExecutor
@Repository
interface TvShowRepository : CrudRepository<TvShow, Int>, JpaSpecificationExecutor<TvShow>


////
// 4. Kotlin Properties are now usable to create fluent specifications
@Service
class MyService @Inject constructor(val tvShowRepo: TvShowRepository) {
   fun findShowsReleasedIn2010NotOnNetflix(): List<TvShow> {
     return tvShowRepo.findAll(TvShow::availableOnNetflix.isFalse() and TvShow::releaseDate.equal("2010"))
   }

   /* Fall back to spring API with some extra helpers for more complex join queries */
   fun findShowsWithComplexQuery(): List<TvShow> {
       return tvShowRepo.findAll(where { equal(it.join(TvShow::starRatings).get(StarRating::stars), 2) })
   }
}
```

# Advanced Usage #

For more complex and dynamic queries it's good practice to create functions that use the DSL to make queries more readable,
and to allow for their composition in complex dynamic queries.

```
fun hasName(name: String?): Specifications<TvShow>? = name?.let {
    TvShow::name.equal(it)
}

fun availableOnNetflix(available: Boolean?): Specifications<TvShow>? = available?.let {
    TvShow::availableOnNetflix.equal(it)
}

fun hasKeywordIn(keywords: List<String>?): Specifications<TvShow>? = keywords?.let {
    or(keywords.map { hasKeyword(it) })
}

fun hasKeyword(keyword: String?): Specifications<TvShow>? = keyword?.let {
    TvShow::synopsis.like("%$keyword%")
}
```

These functions can be combined with and() and or() for complex nested queries:

```
    val shows = tvShowRepo.findAll(
            or(
                    and(
                            availableOnNetflix(false),
                            hasKeywordIn(listOf("Jimmy"))
                    ),
                    and(
                            availableOnNetflix(true),
                            or(
                                    hasKeyword("killer"),
                                    hasKeyword("monster")
                            )
                    )
            )
    )
```

Or they can be combined with a service-layer query DTO and mapping extension function

```
    /**
     * A TV show query DTO - typically used at the service layer.
     */
    data class TvShowQuery(
            val name: String? = null,
            val availableOnNetflix: Boolean? = null,
            val keywords: List<String> = listOf()
    )

    /**
     * A single TvShowQuery is equivalent to an AND of all supplied criteria.
     * Note: any criteria that is null will be ignored (not included in the query).
     */
    fun TvShowQuery.toSpecification(): Specifications<TvShow> = and(
            hasName(name),
            availableOnNetflix(availableOnNetflix),
            hasKeywordIn(keywords)
    )
```

for powerful dynamic queries:

```
    val query = TvShowQuery(availableOnNetflix = false, keywords = listOf("Rick", "Jimmy"))
    val shows = tvShowRepo.findAll(query.toSpecification())
```

For more details, refer to `JPASpecificationDSLTest.kt` in the unit tests.

# How it works #

This DSL builds on [Spring Data's Specifications abstraction](http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications), sprinkling some Kotlin sugar over it to remove the boilerplate and the need to generate a metamodel.

The code `TvShow::releaseDate.equal("2010")` is a call to the Kotlin extension function:

```
fun <T, R> KProperty1<T, R?>.equal(x: R): Specifications<T> = spec { equal(it, x) }
```

This is a bit dense, but makes sense when it's broken down:

- `T`: The type of the object that the property is declared on, in this case TvShow
- `R`: The property type, for TvShow::releaseDate it is String
- `KProperty1<T,R?>`: Kotlin reflection API representation of the property `TvShow::releaseDate`. The 1 refers to a property with 1 receiver, and `R?` is declared as nullable for the method to work on nullable properties as well as non-null properties.
- `x`: The value to test against
- `Specifications<T>`: The Spring data specifications result

This is implemented using a private helper function `spec` that captures the common use case of taking an Entity property, and using a `CriteriaBuilder` to create a `Predicate`:

```
private fun <T, R> KProperty1<T, R?>.spec(makePredicate: CriteriaBuilder.(path: Path<R>) -> Predicate): Specifications<T> =
    this.let { property -> where { root -> makePredicate(root.get(property)) } }
```

This uses the `where` factory method, which expects a callback with the signature: `CriteriaBuilder.(Root<T>) -> Predicate`

The code converts a `KProperty1<T,R>` to a `Path<T>` using `root.get<R>(property)`.

Once it has a `Path<R>` to work with, it delegates to the `makePredicate` function to configure the `CriteriaBuilder` given the `Path`.

The `makePredicate` function passed to `spec` is an extension function on `CriteraiBuilder`. So when `equal(it, x)` is called from inside the `spec` block, it is invoking `CriteriaBuilder::equal`.


# Contributing to the Project #

If you like to contribute code to this project you can do so through GitHub by forking the repository and generating a pull request.

By contributing your code, you agree to license your contribution under the terms of the Apache License v2.0. 