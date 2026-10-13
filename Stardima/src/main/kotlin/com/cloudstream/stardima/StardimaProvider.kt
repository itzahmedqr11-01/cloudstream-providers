package com.cloudstream.stardima

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * CloudStream 3 Provider for Stardima (ستارديما)
 * Official source: https://cm.cartoon.com.im
 * Mirror/Legacy: https://stardima.com / https://cartoon.com.im
 * Specializes in Arabic Dubbed Cartoons, Miraculous Ladybug (ميراكولوس), Disney & Classic Anime
 */
class StardimaProvider : MainAPI() {
    override var mainUrl = "https://cm.cartoon.com.im"
    override var name = "Stardima (كرتون ستارديما)"
    override val hasMainPage = true
    override var lang = "ar"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(
        TvType.Cartoon,
        TvType.Anime,
        TvType.Movie
    )

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    override val mainPage = mainPageOf(
        "$mainUrl/category/miraculous/" to "ميراكولوس مغامرات الدعسوقة (Miraculous)",
        "$mainUrl/category/cartoon-series/" to "مسلسلات كرتون مدبلجة (Dubbed Cartoons)",
        "$mainUrl/category/anime-arabic/" to "أنمي مدبلج ومترجم (Anime Arabic)",
        "$mainUrl/category/cartoon-movies/" to "أفلام كرتون ديزني (Cartoon Movies)",
        "$mainUrl/latest-episodes/" to "أحدث الحلقات المضافة (Latest Episodes)"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(
            request.data,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Accept-Language" to "ar,en;q=0.9",
                "Referer" to "$mainUrl/"
            )
        ).document

        val items = ArrayList<SearchResponse>()

        // Stardima layout selectors: article cards, video posts, thumbnails
        val selectors = "article.item-list, .post-item, .video-block, .box-item, article.post"
        document.select(selectors).forEach { element ->
            val titleEl = element.selectFirst("h2.title a, h3.title a, .entry-title a, a.post-title")
            val title = titleEl?.text() ?: element.selectFirst("img")?.attr("alt") ?: return@forEach
            val href = titleEl?.attr("href") ?: element.selectFirst("a")?.attr("href") ?: return@forEach
            val poster = element.selectFirst("img")?.attr("data-src")
                ?: element.selectFirst("img")?.attr("src")

            items.add(
                newAnimeSearchResponse(title.trim(), fixUrl(href), TvType.Cartoon) {
                    this.posterUrl = fixUrlNull(poster)
                    addDub(null) // Stardima is primarily Arabic dub
                }
            )
        }

        return newHomePageResponse(
            listOf(HomePageList(request.name, items, isHorizontalImages = false)),
            hasNext = document.select(".pagination .next, a.next").isNotEmpty()
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=${query.replace(" ", "+")}"
        val document = app.get(
            searchUrl,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "$mainUrl/"
            )
        ).document

        val results = ArrayList<SearchResponse>()
        document.select("article, .post-item, .search-result, .item-list").forEach { el ->
            val title = el.selectFirst("h2 a, h3 a, .title a")?.text() ?: return@forEach
            val href = el.selectFirst("a")?.attr("href") ?: return@forEach
            val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")

            results.add(
                newAnimeSearchResponse(title.trim(), fixUrl(href), TvType.Cartoon) {
                    this.posterUrl = fixUrlNull(poster)
                }
            )
        }
        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(
            url,
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "$mainUrl/"
            )
        ).document

        val title = document.selectFirst("h1.post-title, h1.entry-title, .single-title, h1")?.text()
            ?: "حلقة كرتون ستارديما"
        val poster = document.selectFirst(".single-poster img, .entry-content img, meta[property='og:image']")?.attr("src")
        val plot = document.selectFirst(".entry-content p, .post-content, .description")?.text()

        val episodes = ArrayList<Episode>()

        // Check if page is a series page with episode list
        val episodeLinks = document.select(".episodes-list a, .playlist-items a, .series-episodes a")
        if (episodeLinks.isNotEmpty()) {
            episodeLinks.forEachIndexed { index, el ->
                val epHref = el.attr("href")
                val epTitle = el.text().ifEmpty { "الحلقة ${index + 1}" }
                val epNum = epTitle.filter { it.isDigit() }.toIntOrNull() ?: (index + 1)

                episodes.add(
                    newEpisode(fixUrl(epHref)) {
                        this.name = epTitle
                        this.episode = epNum
                    }
                )
            }
        } else {
            // Single video post / direct episode post
            episodes.add(
                newEpisode(url) {
                    this.name = title
                    this.episode = 1
                }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Cartoon) {
            this.posterUrl = fixUrlNull(poster)
            this.plot = plot
            this.tags = listOf("كرتون مدبلج", "ستارديما", "أطفال وناشئة")
            this.episodes = mutableMapOf(DubStatus.Dubbed to episodes)
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
            headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to "$mainUrl/"
            )
        ).document

        // Check embedded video players and external mirrors on Stardima
        val iframes = document.select("iframe[src], .video-embed iframe, .player-frame iframe")
        iframes.forEach { iframe ->
            var src = iframe.attr("src")
            if (src.startsWith("//")) src = "https:$src"

            when {
                src.contains("dood") || src.contains("ds2play") || src.contains("doodstream") -> {
                    loadExtractor(src, subtitleCallback, callback)
                }
                src.contains("streamwish") || src.contains("wishfast") -> {
                    loadExtractor(src, subtitleCallback, callback)
                }
                src.contains("ok.ru") || src.contains("odnoklassniki") -> {
                    loadExtractor(src, subtitleCallback, callback)
                }
                src.contains("mp4upload") -> {
                    loadExtractor(src, subtitleCallback, callback)
                }
                src.contains("drive.google.com") -> {
                    // Google Drive video player extractor
                    callback.invoke(
                        ExtractorLink(
                            name,
                            "Stardima Google Drive Server",
                            src,
                            "$mainUrl/",
                            Qualities.P720.value
                        )
                    )
                }
                src.contains(".mp4") || src.contains(".m3u8") -> {
                    callback.invoke(
                        ExtractorLink(
                            name,
                            "Stardima Direct Server",
                            src,
                            "$mainUrl/",
                            Qualities.P1080.value,
                            isM3u8 = src.contains(".m3u8")
                        )
                    )
                }
            }
        }

        // Direct video tag sources
        document.select("video source").forEach { source ->
            val videoUrl = source.attr("src")
            if (videoUrl.isNotEmpty()) {
                callback.invoke(
                    ExtractorLink(
                        name,
                        "Stardima Direct Stream",
                        fixUrl(videoUrl),
                        "$mainUrl/",
                        Qualities.P720.value
                    )
                )
            }
        }

        return true
    }
}
