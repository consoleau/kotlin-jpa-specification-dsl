package au.com.console.jpaspecificationdsl

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.data.jpa.domain.Specifications
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional


@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(JPASpecificationDSLTest::class))
@SpringBootApplication
@Transactional
open class JPASpecificationDSLTest {

    @Autowired
    lateinit var tvShowRepo: TvShowRepository

    lateinit var hemlockGrove: TvShow
    lateinit var theWalkingDead: TvShow
    lateinit var betterCallSaul: TvShow

    @Before
    fun setup() {
        with(tvShowRepo) {
            hemlockGrove = save(
                    TvShow(
                            name = "Hemlock Grove",
                            availableOnNetflix = true,
                            synopsis = "A teenage girl is brutally murdered, sparking a hunt for her killer. But in a town where everyone hides a secret, will they find the monster among them?",
                            releaseDate = "2013"))

            theWalkingDead = save(
                    TvShow(
                            name = "The Walking Dead",
                            availableOnNetflix = false,
                            synopsis = "Sheriff Deputy Rick Grimes leads a group of survivors in a world overrun by the walking dead. Fighting the dead, fearing the living.",
                            releaseDate = "2010",
                            starRatings = setOf(StarRating(stars = 3), StarRating(stars = 4))))

            betterCallSaul = save(
                    TvShow(
                            name = "Better Call Saul",
                            availableOnNetflix = false,
                            synopsis = "The trials and tribulations of criminal lawyer, Jimmy McGill, in the time leading up to establishing his strip-mall law office in Albuquerque, New Mexico.",
                            starRatings = setOf(StarRating(stars = 4), StarRating(stars = 2))))
        }
    }

    @After
    fun tearDown() {
        tvShowRepo.deleteAll()
    }

    /**
     * A TV show query DTO - typically used at the service layer.
     */
    data class TvShowQuery(
            val name: String? = null,
            val availableOnNetflix: Boolean? = null,
            val keywords: List<String> = listOf(),
            val releaseDates: List<String> = listOf()
    )

    /**
     * A single TvShowQuery is equivalent to an AND of all supplied criteria.
     * Note: any criteria that is null will be ignored (not included in the query).
     */
    fun TvShowQuery.toSpecification(): Specifications<TvShow> = and(
            hasName(name),
            availableOnNetflix(availableOnNetflix),
            hasKeywordIn(keywords),
            hasReleaseDateIn(releaseDates)
    )

    /**
     * A collection of TvShowQueries is equivalent to an OR of all the queries in the collection.
     */
    fun Iterable<TvShowQuery>.toSpecification(): Specifications<TvShow> = or(
            map { query -> query.toSpecification() }
    )

    @Test
    fun `Get a tv show by id`() {
        val show = tvShowRepo.findOne(hemlockGrove.id)
        assertThat(show, equalTo(hemlockGrove))
    }

    @Test
    fun `Get a tv show by id equality`() {
        val show = tvShowRepo.findOne(TvShow::id.equal(theWalkingDead.id))
        assertThat(show, equalTo(theWalkingDead))
    }

    @Test
    fun `Get tv shows by id notEqual`() {
        val shows = tvShowRepo.findAll(TvShow::name.notEqual(theWalkingDead.name))
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove))
    }

    @Test
    fun `Get tv show by id in`() {
        val shows = tvShowRepo.findAll(TvShow::id.`in`(setOf(hemlockGrove.id, theWalkingDead.id)))
        assertThat(shows, containsInAnyOrder(hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Get tv show by id lt`() {
        val shows = tvShowRepo.findAll(TvShow::id.lt(betterCallSaul.id))
        assertThat(shows, containsInAnyOrder(hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Get tv show by id le`() {
        val shows = tvShowRepo.findAll(TvShow::id.le(theWalkingDead.id))
        assertThat(shows, containsInAnyOrder(hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Get tv show by id gt`() {
        val shows = tvShowRepo.findAll(TvShow::id.gt(hemlockGrove.id))
        assertThat(shows, containsInAnyOrder(theWalkingDead, betterCallSaul))
    }

    @Test
    fun `Get tv show by id ge`() {
        val shows = tvShowRepo.findAll(TvShow::id.ge(theWalkingDead.id))
        assertThat(shows, containsInAnyOrder(theWalkingDead, betterCallSaul))
    }

    @Test
    fun `Get tv show by name lessThan`() {
        val shows = tvShowRepo.findAll(TvShow::name.lessThan("C"))
        assertThat(shows, containsInAnyOrder(betterCallSaul))
    }

    @Test
    fun `Get tv show by name lessThanOrEqualTo`() {
        val shows = tvShowRepo.findAll(TvShow::name.lessThanOrEqualTo("Hemlock Grove"))
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove))
    }

    @Test
    fun `Get tv show by name greaterThan`() {
        val shows = tvShowRepo.findAll(TvShow::name.greaterThan("Hemlock Grove"))
        assertThat(shows, containsInAnyOrder(theWalkingDead))
    }

    @Test
    fun `Get tv show by name greaterThanOrEqualTo`() {
        val shows = tvShowRepo.findAll(TvShow::name.greaterThanOrEqualTo("Hemlock Grove"))
        assertThat(shows, containsInAnyOrder(hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Get tv show by name between`() {
        val shows = tvShowRepo.findAll(TvShow::name.between("A", "H"))
        assertThat(shows, containsInAnyOrder(betterCallSaul))
    }

    @Test
    fun `Get tv show by boolean isTrue`() {
        val shows = tvShowRepo.findAll(TvShow::availableOnNetflix.isTrue())
        assertThat(shows, containsInAnyOrder(hemlockGrove))
    }

    @Test
    fun `Get tv show by boolean isFalse`() {
        val shows = tvShowRepo.findAll(TvShow::availableOnNetflix.isFalse())
        assertThat(shows, containsInAnyOrder(betterCallSaul, theWalkingDead))
    }

    @Test
    fun `Get tv show by releaseDate isNull`() {
        val shows = tvShowRepo.findAll(TvShow::releaseDate.isNull())
        assertThat(shows, containsInAnyOrder(betterCallSaul))
    }

    @Test
    fun `Get tv show by releaseDate isNotNull`() {
        val shows = tvShowRepo.findAll(TvShow::releaseDate.isNotNull())
        assertThat(shows, containsInAnyOrder(hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Get tv show by ratings isEmpty`() {
        val shows = tvShowRepo.findAll(TvShow::starRatings.isEmpty())
        assertThat(shows, containsInAnyOrder(hemlockGrove))
    }

    @Test
    fun `Get tv show by ratings isNotEmpty`() {
        val shows = tvShowRepo.findAll(TvShow::starRatings.isNotEmpty())
        assertThat(shows, containsInAnyOrder(betterCallSaul, theWalkingDead))
    }

    @Test
    fun `Get tv show by isMember`() {
        val shows = tvShowRepo.findAll(TvShow::starRatings.isMember(theWalkingDead.starRatings.first()))
        assertThat(shows, containsInAnyOrder(theWalkingDead))
    }

    @Test
    fun `Get tv show by isNotMember`() {
        val shows = tvShowRepo.findAll(TvShow::starRatings.isNotMember(betterCallSaul.starRatings.first()))
        assertThat(shows, containsInAnyOrder(theWalkingDead, hemlockGrove))
    }

    @Test
    fun `Get a tv show by name like`() {
        val shows = tvShowRepo.findAll(TvShow::name.like("The%"))
        assertThat(shows, containsInAnyOrder(theWalkingDead))
    }

    @Test
    fun `Get a tv show by synopsis like with escape char`() {
        val shows = tvShowRepo.findAll(TvShow::synopsis.like("%them\\?", escapeChar = '\\'))
        assertThat(shows, containsInAnyOrder(hemlockGrove))
    }

    @Test
    fun `Get a tv show by name notLike`() {
        val shows = tvShowRepo.findAll(TvShow::name.notLike("The %"))
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove))
    }

    @Test
    fun `Get a tv show by synopsis notLike with escape char`() {
        val shows = tvShowRepo.findAll(TvShow::synopsis.notLike("%\\.", escapeChar = '\\'))
        assertThat(shows, containsInAnyOrder(hemlockGrove))
    }

    @Test
    fun `Find tv shows with and`() {
        val shows = tvShowRepo.findAll(TvShow::availableOnNetflix.isFalse() and TvShow::releaseDate.equal("2010"))
        assertThat(shows, containsInAnyOrder(theWalkingDead))
    }

    @Test
    fun `Find tv shows with or`() {
        val shows = tvShowRepo.findAll(TvShow::availableOnNetflix.isTrue() or TvShow::releaseDate.equal("2010"))
        assertThat(shows, containsInAnyOrder(hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Find tv shows with not operator`() {
        val shows = tvShowRepo.findAll(!TvShow::releaseDate.equal("2010"))
        assertThat(shows, containsInAnyOrder(hemlockGrove))
    }

    @Test
    fun `Test Join`() {
        val shows = tvShowRepo.findAll(where { equal(it.join(TvShow::starRatings).get(StarRating::stars), 2) })
        assertThat(shows, containsInAnyOrder(betterCallSaul))
    }

    @Test
    fun `Find tv shows by query DTO`() {
        val query = TvShowQuery(availableOnNetflix = false, keywords = listOf("Rick", "Jimmy"))
        val shows = tvShowRepo.findAll(query.toSpecification())
        assertThat(shows, containsInAnyOrder(betterCallSaul, theWalkingDead))
    }

    @Test
    fun `Find tv shows by query DTO - empty query`() {
        val query = TvShowQuery()
        val shows = tvShowRepo.findAll(query.toSpecification())
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Find tv shows by multiple query DTOs`() {
        val queries = listOf(
                TvShowQuery(availableOnNetflix = false, keywords = listOf("Jimmy")),
                TvShowQuery(availableOnNetflix = true, keywords = listOf("killer", "monster"), releaseDates = listOf("2010", "2013"))
        )
        val shows = tvShowRepo.findAll(queries.toSpecification())
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove))
    }

    @Test
    fun `Find tv shows by empty query DTOs list`() {
        val queries = listOf<TvShowQuery>()
        val shows = tvShowRepo.findAll(queries.toSpecification())
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove, theWalkingDead))
    }

    @Test
    fun `Find tv shows by inlined query`() {
        val shows = tvShowRepo.findAll(and(
                availableOnNetflix(false),
                hasKeywordIn(listOf("Rick", "Jimmy"))
        ))
        assertThat(shows, containsInAnyOrder(betterCallSaul, theWalkingDead))
    }

    @Test
    fun `Find tv shows by complex inlined query`() {
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
        assertThat(shows, containsInAnyOrder(betterCallSaul, hemlockGrove))
    }

}
