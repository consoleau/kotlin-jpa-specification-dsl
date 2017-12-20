package au.com.console.jpaspecificationdsl

import org.springframework.data.jpa.domain.Specifications
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToMany

@Repository
interface TvShowRepository : CrudRepository<TvShow, Int>, JpaSpecificationExecutor<TvShow>

interface HasName {
    val name: String
}

@Entity
data class TvShow (
        @Id
        @GeneratedValue
        val id: Int = 0,
        override val name: String = "",
        val synopsis: String = "",
        val availableOnNetflix: Boolean = false,
        val releaseDate: String? = null,
        @OneToMany(cascade = kotlin.arrayOf(javax.persistence.CascadeType.ALL))
        val starRatings: Set<StarRating> = emptySet()): HasName

@Entity
data class StarRating(
        @Id
        @GeneratedValue
        val id: Int = 0,
        val stars: Int = 0)


// Convenience functions (using the DSL) that make assembling queries more readable and allows for dynamic queries.
// Note: these functions return null for a null input. This means that when included in
// and() or or() they will be ignored as if they weren't supplied.

fun hasName(name: String?): Specifications<HasName>? = name?.let {
    HasName::name.equal(it)
}

fun availableOnNetflix(available: Boolean?): Specifications<TvShow>? = available?.let {
    TvShow::availableOnNetflix.equal(it)
}

fun hasReleaseDateIn(releaseDates: List<String>?): Specifications<TvShow>? = releaseDates?.let {
    TvShow::releaseDate.`in`(releaseDates)
}

fun hasKeywordIn(keywords: List<String>?): Specifications<TvShow>? = keywords?.let {
    or(keywords.map(::hasKeyword))
}

fun hasKeyword(keyword: String?): Specifications<TvShow>? = keyword?.let {
    TvShow::synopsis.like("%$keyword%")
}
