package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class UFOGrendizerProvider : MainAPI() {
    override var mainUrl = "https://ufogrendizer.tv"
    override var name = "UFO Grendizer"
    override var lang = "ar"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime)

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val lists = document.select("div.episodes-card")
        
        val items = lists.map { element ->
            val title = element.selectFirst("h3.episode-title")?.text() ?: ""
            val poster = element.selectFirst("img")?.attr("src")
            val href = fixUrl(element.selectFirst("a")?.attr("href") ?: "")
            
            newAnimeSearchResponse(
                name = title,
                url = href,
                type = TvType.Anime
            ) {
                this.posterUrl = poster
            }
        }

        return HomePageResponse(
            listOf(HomePageList("Latest Episodes", items))
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchUrl = "$mainUrl/?s=$query"
        val document = app.get(searchUrl).document
        
        return document.select("div.episodes-card").map { element ->
            val title = element.selectFirst("h3.episode-title")?.text() ?: ""
            val href = fixUrl(element.selectFirst("a")?.attr("href") ?: "")
            val poster = element.selectFirst("img")?.attr("src")
            
            newAnimeSearchResponse(title, href, TvType.Anime) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        
        val title = document.selectFirst("h1.entry-title")?.text() ?: ""
        val poster = document.selectFirst("div.thumb img")?.attr("src")
        val description = document.selectFirst("div.entry-content p")?.text()
        
        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            addEpisodes(DubStatus.Subbed, listOf(
                Episode(
                    url,
                    "Episode",
                )
            ))
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val videoElements = document.select("iframe[src], source[src]")
        
        videoElements.forEach { element ->
            val url = element.attr("src")
            if (url.isNotEmpty()) {
                callback.invoke(
                    ExtractorLink(
                        name,
                        name,
                        url,
                        mainUrl,
                        Qualities.Unknown.value
                    )
                )
            }
        }
        
        return true
    }
}