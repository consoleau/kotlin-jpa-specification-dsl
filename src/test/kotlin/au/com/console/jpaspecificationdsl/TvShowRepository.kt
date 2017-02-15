package au.com.console.jpaspecificationdsl

import org.springframework.data.jpa.domain.Specifications
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.*
import javax.persistence.criteria.JoinType

@Repository
interface TvShowRepository : CrudRepository<TvShow, Int>, JpaSpecificationExecutor<TvShow>

@Entity
data class TvShow(
        @Id
        @GeneratedValue
        val id: Int = 0,
        val name: String = "",
        val synopsis: String = "",
        val availableOnNetflix: Boolean = false,
        val releaseDate: String? = null,
        @OneToMany(cascade = kotlin.arrayOf(javax.persistence.CascadeType.ALL), fetch = FetchType.LAZY)
        @JoinColumn(name = "tv_show_id", nullable = false, updatable = false)
        val starRatings: Set<StarRating> = emptySet())

@Entity
data class StarRating(
        @Id
        @GeneratedValue
        val id: Int = 0,
        val stars: Int = 0)


// Convenience functions (using the DSL) that make assembling queries more readable and allows for dynamic queries.
// Note: these functions return null for a null input. This means that when included in
// and() or or() they will be ignored as if they weren't supplied.

fun hasName(name: String?): Specifications<TvShow>? = name?.let {
    TvShow::name.equal(it)
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

fun fetchRatings(excludeShowsWithoutRatings: Boolean = true) : Specifications<TvShow> = where(applyQuery = { distinct(true) }) {
    it.fetch(prop = TvShow::starRatings, joinType = if (excludeShowsWithoutRatings) JoinType.INNER else JoinType.LEFT)
    and() // we need a predicate
}
