package com.cloudstream.animaura

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * CloudStream 3 Provider for Animaura
 * Official source: https://www.animaura.xyz
 * Direct focus: https://www.animaura.xyz/anime/miraculous
 */
class AnimauraProvider : MainAPI() {
    override var mainUrl = "https://www.animaura.xyz"
    override var name = "Animaura"
    override val hasMainPage = true
    override var lang = "en"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.Cartoon,
        TvType.AnimeMovie
    )

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }

    override val mainPage = mainPageOf(
        "$mainUrl/anime/miraculous" to "Miraculous Ladybug Universe",
        "$mainUrl/trending" to "Trending Now",
        "$mainUrl/popular" to "Popular Anime",
        "$mainUrl/recent-episodes" to "Recent Episodes",
        "$mainUrl/movies" to "Anime Movies"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(
            request.data,
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to "$mainUrl/")
        ).document

        val homeItems = ArrayList<SearchResponse>()

        // Check if page requested is the direct Miraculous series portal
        if (request.data.contains("/anime/miraculous")) {
            val title = document.selectFirst("h1.anime-title, .entry-title, .title, meta[property='og:title']")
                ?.text() ?: "Miraculous: Tales of Ladybug & Cat Noir"
            val poster = document.selectFirst("div.poster img, .anime-poster img, meta[property='og:image']")
                ?.attr("src") ?: "$mainUrl/images/miraculous-poster.jpg"
            val link = request.data

            homeItems.add(
                newAnimeSearchResponse(title, link, TvType.Cartoon) {
                    this.posterUrl = fixUrlNull(poster)
                    addSub(130)
                    addDub(130)
                }
            )

            // Parse related Miraculous films / specials on the page
            document.select(".related-grid .anime-card, .series-item, .franchise-entry, .season-card").forEach { card ->
                val cardTitle = card.selectFirst(".title, h3, h4")?.text() ?: return@forEach
                val cardHref = card.selectFirst("a")?.attr("href") ?: return@forEach
                val cardPoster = card.selectFirst("img")?.attr("src")
                homeItems.add(
                    newAnimeSearchResponse(cardTitle, fixUrl(cardHref), TvType.Cartoon) {
                        this.posterUrl = fixUrlNull(cardPoster)
                    }
                )
            }
        } else {
            // Standard category scraping
            val selector = ".anime-card, .film-item, .film_list-wrap .flw-item, article.post"
            document.select(selector).forEach { element ->
                val title = element.selectFirst(".film-name a, .entry-title a, .title a, h3 a")?.text()
                    ?: element.selectFirst("img")?.attr("alt") ?: return@forEach
                val href = element.selectFirst("a")?.attr("href") ?: return@forEach
                val poster = element.selectFirst("img")?.attr("data-src")
                    ?: element.selectFirst("img")?.attr("src")

                val dubCount = element.selectFirst(".tick-dub")?.text()?.toIntOrNull()
                val subCount = element.selectFirst(".tick-sub")?.text()?.toIntOrNull()

                homeItems.add(
                    newAnimeSearchResponse(title, fixUrl(href), TvType.Anime) {
                        this.posterUrl = fixUrlNull(poster)
                        addSub(subCount)
                        addDub(dubCount)
                    }
                )
            }
        }

        return newHomePageResponse(
            listOf(HomePageList(request.name, homeItems, isHorizontalImages = false)),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/search?keyword=${query.replace(" ", "+")}"
        val document = app.get(
            searchUrl,
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to "$mainUrl/")
        ).document

        val results = ArrayList<SearchResponse>()
        document.select(".anime-card, .film-item, .search-result-item, .flw-item").forEach { element ->
            val title = element.selectFirst(".title, .film-name, h3, a.dynamic-name")?.text() ?: return@forEach
            val href = element.selectFirst("a")?.attr("href") ?: return@forEach
            val poster = element.selectFirst("img")?.attr("data-src")
                ?: element.selectFirst("img")?.attr("src")
            val isDub = element.selectFirst(".tick-dub") != null

            results.add(
                newAnimeSearchResponse(title, fixUrl(href), TvType.Anime) {
                    this.posterUrl = fixUrlNull(poster)
                    if (isDub) addDub(null) else addSub(null)
                }
            )
        }
        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(
            url,
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to "$mainUrl/")
        ).document

        val title = document.selectFirst("h1.anime-title, .heading-large, .film-name, h1")?.text()
            ?: "Miraculous: Tales of Ladybug & Cat Noir"
        val poster = document.selectFirst(".anime-poster img, .film-poster img, meta[property='og:image']")?.attr("src")
        val banner = document.selectFirst(".anime-banner img, .film-cover img, meta[property='og:image']")?.attr("src")
        val plot = document.selectFirst(".anime-synopsis, .description, .synopsis, .film-description")?.text()
        val year = document.selectFirst(".anime-info .year, .film-item-year, .published")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = document.select(".anime-genres a, .genres a, .film-genres a").map { it.text() }

        val episodes = ArrayList<Episode>()

        // Miraculous Season & Episode tab parsing
        val seasonElements = document.select(".season-tab, .seasons-list li, .dropdown-season option")
        val episodeElements = document.select(".episode-item, .ssl-item, .episodes-list a, .ep-item")

        if (episodeElements.isNotEmpty()) {
            episodeElements.forEachIndexed { index, el ->
                val epHref = el.attr("href")
                val epNum = el.selectFirst(".ep-number, .num")?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: (index + 1)
                val epName = el.selectFirst(".ep-title, .title, .name")?.text() ?: "Episode $epNum"
                val epSeason = el.attr("data-season").toIntOrNull() ?: 1

                episodes.add(
                    newEpisode(fixUrl(epHref)) {
                        this.name = epName
                        this.episode = epNum
                        this.season = epSeason
                    }
                )
            }
        } else {
            // Fallback: If page has servers directly embedded
            episodes.add(
                newEpisode(url) {
                    this.name = "Full Episode / Special"
                    this.episode = 1
                    this.season = 1
                }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = fixUrlNull(poster)
            this.backgroundPosterUrl = fixUrlNull(banner)
            this.year = year
            this.plot = plot
            this.tags = tags
            this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(
            data,
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to "$mainUrl/")
        ).document

        // Extract streaming iframes and server buttons
        val servers = document.select("iframe[src], .servers-list .server, .stream-player iframe, div[data-src]")

        servers.forEach { server ->
            var streamUrl = server.attr("src").ifEmpty { server.attr("data-src") }
            if (streamUrl.startsWith("//")) streamUrl = "https:$streamUrl"

            when {
                streamUrl.contains("streamtape.com") || streamUrl.contains("streamtape.net") -> {
                    loadExtractor(streamUrl, subtitleCallback, callback)
                }
                streamUrl.contains("filemoon") || streamUrl.contains("moonplayer") -> {
                    loadExtractor(streamUrl, subtitleCallback, callback)
                }
                streamUrl.contains("vidstream") || streamUrl.contains("megacloud") -> {
                    loadExtractor(streamUrl, subtitleCallback, callback)
                }
                streamUrl.contains("mp4upload.com") -> {
                    loadExtractor(streamUrl, subtitleCallback, callback)
                }
                streamUrl.contains(".m3u8") -> {
                    callback.invoke(
                        ExtractorLink(
                            name,
                            "Animaura HLS Server",
                            streamUrl,
                            "$mainUrl/",
                            Qualities.P1080.value,
                            isM3u8 = true
                        )
                    )
                }
            }
        }

        return true
    }
}
