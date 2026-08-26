package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.ScreenPreset

object SampleScreenGenerator {

    val PRESETS = listOf(
        ScreenPreset(
            id = "news_article",
            title = "最新ニュース",
            category = "記事・メディア",
            description = "AI技術の最新動向と日本語解説記事",
            primaryColor = 0xFF1E3A8A
        ),
        ScreenPreset(
            id = "social_post",
            title = "SNSフォト投稿",
            category = "ソーシャル",
            description = "旅行の写真とキャプション付きタイムライン",
            primaryColor = 0xFF047857
        ),
        ScreenPreset(
            id = "product_spec",
            title = "商品スペック・EC",
            category = "ショッピング",
            description = "ガジェットの価格・スペック比較表",
            primaryColor = 0xFF7C3AED
        ),
        ScreenPreset(
            id = "travel_hotel",
            title = "旅行予約・観光",
            category = "トラベル",
            description = "京都観光ガイド・ホテル予約情報",
            primaryColor = 0xFFB91C1C
        )
    )

    fun createPresetBitmap(presetId: String, width: Int = 1080, height: Int = 2200): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (presetId) {
            "social_post" -> drawSocialPostScreen(canvas, width, height)
            "product_spec" -> drawProductSpecScreen(canvas, width, height)
            "travel_hotel" -> drawTravelHotelScreen(canvas, width, height)
            else -> drawNewsArticleScreen(canvas, width, height)
        }

        return bitmap
    }

    private fun drawNewsArticleScreen(canvas: Canvas, w: Int, h: Int) {
        // Background
        canvas.drawColor(Color.parseColor("#F8FAFC"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Status & Header bar
        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, w.toFloat(), 180f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TechNews Japan - 総合ヘッドライン", 60f, 120f, paint)

        // Article Category Tag
        paint.color = Color.parseColor("#3B82F6")
        val tagRect = RectF(60f, 220f, 240f, 280f)
        canvas.drawRoundRect(tagRect, 16f, 16f, paint)
        paint.color = Color.WHITE
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AI・最新技術", 78f, 262f, paint)

        // Date
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 32f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("2026年8月26日 14:30 更新", 270f, 262f, paint)

        // Article Headline
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 62f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("次世代AIアシスタント機能が登場", 60f, 360f, paint)
        canvas.drawText("画面のなぞり検索と文字認識が進化", 60f, 435f, paint)

        // Hero Graphic Box (Simulated Image)
        val heroRect = RectF(60f, 480f, w - 60f, 1050f)
        val heroGradient = LinearGradient(
            heroRect.left, heroRect.top, heroRect.right, heroRect.bottom,
            Color.parseColor("#1E3A8A"), Color.parseColor("#3B82F6"),
            Shader.TileMode.CLAMP
        )
        paint.shader = heroGradient
        canvas.drawRoundRect(heroRect, 28f, 28f, paint)
        paint.shader = null

        // Inner glowing circle illustration in hero
        paint.color = Color.parseColor("#60A5FA")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawCircle(w / 2f, 740f, 180f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Circle to Search OCR", w / 2f, 740f, paint)
        paint.textSize = 34f
        paint.color = Color.parseColor("#93C5FD")
        canvas.drawText("なぞって選択・円で囲んで切り抜き", w / 2f, 800f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Image Caption
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 32f
        canvas.drawText("▲ スマートフォン上でのOCR認識とシェア機能のイメージ", 60f, 1100f, paint)

        // Body Text Paragraphs
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 42f
        paint.typeface = Typeface.DEFAULT

        val lines = listOf(
            "スマートフォン操作中に気になるテキストや画像を、",
            "ホームボタンの長押しから即座にOCR認識できる",
            "新しいアシスタント機能が大きな注目を集めています。",
            "",
            "画面上を指でなぞるだけで、高精度な日本語・英語の",
            "テキストを素早く選択してクリップボードにコピーしたり、",
            "よく使うアプリへワンタップでシェア可能です。",
            "",
            "さらに画面上の写真やイラストを丸く囲むと、",
            "自動で切り抜き枠が生成され、他のSNSやチャットに",
            "瞬時に画像として転送できるよう設計されています。"
        )

        var y = 1190f
        for (line in lines) {
            if (line.isNotEmpty()) {
                canvas.drawText(line, 60f, y, paint)
            }
            y += 58f
        }

        // Summary Card at Bottom
        val summaryRect = RectF(60f, y + 40f, w - 60f, y + 260f)
        paint.color = Color.parseColor("#EFF6FF")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(summaryRect, 24f, 24f, paint)
        paint.color = Color.parseColor("#3B82F6")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(summaryRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E40AF")
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("💡 主な特徴と操作方法", 90f, y + 110f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 34f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("・指で文字をなぞって選択 ➜ コピー / シェア", 90f, y + 170f, paint)
        canvas.drawText("・画像を円で囲む ➜ 切り抜いて送信", 90f, y + 220f, paint)
    }

    private fun drawSocialPostScreen(canvas: Canvas, w: Int, h: Int) {
        canvas.drawColor(Color.parseColor("#F1F5F9"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Navigation Bar
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, w.toFloat(), 180f, paint)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 48f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("タイムライン - 写真＆日記", 60f, 120f, paint)

        // Post Card
        val cardRect = RectF(40f, 220f, w - 40f, 1950f)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 32f, 32f, paint)

        // Author Avatar
        paint.color = Color.parseColor("#059669")
        canvas.drawCircle(120f, 300f, 48f, paint)
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("T", 120f, 314f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Author Name & Time
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("田中 健太 @kenta_tokyo", 190f, 290f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 32f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("30分前 ・ 東京都 渋谷区", 190f, 335f, paint)

        // Post Text
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 44f
        canvas.drawText("新しくオープンしたカフェに行ってきました！☕", 70f, 420f, paint)
        canvas.drawText("抹茶ラテと自家製チーズケーキが絶品でした。", 70f, 480f, paint)
        canvas.drawText("おすすめスポット: Cafe Hikari 渋谷店", 70f, 540f, paint)
        canvas.drawText("営業時間 10:00 - 21:00 (年中無休)", 70f, 600f, paint)

        // Photo Grid in Post
        val photo1 = RectF(70f, 650f, w - 70f, 1350f)
        val photoGrad = LinearGradient(
            photo1.left, photo1.top, photo1.right, photo1.bottom,
            Color.parseColor("#10B981"), Color.parseColor("#047857"),
            Shader.TileMode.CLAMP
        )
        paint.shader = photoGrad
        canvas.drawRoundRect(photo1, 24f, 24f, paint)
        paint.shader = null

        // Camera graphic details
        paint.color = Color.WHITE
        paint.textSize = 56f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("🍵 抹茶ラテ & ケーキ", w / 2f, 980f, paint)
        paint.textSize = 36f
        paint.color = Color.parseColor("#A7F3D0")
        canvas.drawText("（円で囲むとこの写真だけ切り抜けます）", w / 2f, 1050f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Post Comments & Engagement
        paint.color = Color.parseColor("#334155")
        paint.textSize = 38f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("❤️ 142 いいね   💬 18 件のコメント   🔁 24 リポスト", 70f, 1420f, paint)

        // Divider
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 2f
        canvas.drawLine(70f, 1470f, w - 70f, 1470f, paint)

        // Comment 1
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("佐藤 美咲: ここの抹茶ラテ本当に美味しいよね！", 70f, 1530f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 32f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("週末に友達と行ってみます✨", 70f, 1580f, paint)

        // Comment 2
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 38f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("鈴木 翔太: 写真の切り抜き送ってくれてありがとう！", 70f, 1660f, paint)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 32f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("住所教えて: 渋谷区神南1-22-8 3F", 70f, 1710f, paint)
    }

    private fun drawProductSpecScreen(canvas: Canvas, w: Int, h: Int) {
        canvas.drawColor(Color.parseColor("#FAFAFA"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        paint.color = Color.parseColor("#18181B")
        canvas.drawRect(0f, 0f, w.toFloat(), 180f, paint)
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Gadget Store - 製品詳細スペック", 60f, 120f, paint)

        // Product Banner
        val bannerRect = RectF(60f, 220f, w - 60f, 820f)
        val bannerGrad = LinearGradient(
            bannerRect.left, bannerRect.top, bannerRect.right, bannerRect.bottom,
            Color.parseColor("#6D28D9"), Color.parseColor("#4C1D95"),
            Shader.TileMode.CLAMP
        )
        paint.shader = bannerGrad
        canvas.drawRoundRect(bannerRect, 32f, 32f, paint)
        paint.shader = null

        paint.color = Color.WHITE
        paint.textSize = 64f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Pro Wireless Earbuds X2", 100f, 360f, paint)

        paint.color = Color.parseColor("#DDD6FE")
        paint.textSize = 42f
        canvas.drawText("アクティブノイズキャンセリング搭載", 100f, 430f, paint)

        paint.color = Color.parseColor("#FDE047")
        paint.textSize = 72f
        canvas.drawText("特別価格: ¥24,800 (税込)", 100f, 540f, paint)

        paint.color = Color.WHITE
        paint.textSize = 36f
        canvas.drawText("通常価格 ¥29,800 より 17% OFF", 100f, 610f, paint)
        canvas.drawText("送料無料 ・ 翌日お届け対応", 100f, 670f, paint)

        // Specs Table Box
        val tableRect = RectF(60f, 870f, w - 60f, 1750f)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(tableRect, 24f, 24f, paint)
        paint.color = Color.parseColor("#E4E4E7")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(tableRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        // Table Rows
        paint.color = Color.parseColor("#18181B")
        paint.textSize = 44f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("主要仕様・スペック表", 100f, 950f, paint)

        val specs = listOf(
            "Bluetooth バージョン" to "Bluetooth 5.4 / LE Audio",
            "バッテリー駆動時間" to "単体 8時間 / ケース込 36時間",
            "充電方式" to "USB Type-C / Qiワイヤレス充電",
            "防水・防塵規格" to "IPX5 生活防水規格対応",
            "対応コーデック" to "LDAC, AAC, SBC, LC3",
            "重量" to "イヤホン単体 4.8g / ケース 42g",
            "保証期間" to "購入後 1年間 メーカー正規保証"
        )

        var rowY = 1040f
        paint.textSize = 38f
        for ((k, v) in specs) {
            paint.color = Color.parseColor("#71717A")
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(k, 100f, rowY, paint)

            paint.color = Color.parseColor("#18181B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(v, 480f, rowY, paint)

            // Divider line
            paint.color = Color.parseColor("#F4F4F5")
            paint.strokeWidth = 2f
            canvas.drawLine(100f, rowY + 30f, w - 100f, rowY + 30f, paint)

            rowY += 100f
        }
    }

    private fun drawTravelHotelScreen(canvas: Canvas, w: Int, h: Int) {
        canvas.drawColor(Color.parseColor("#FFFBEB"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        paint.color = Color.parseColor("#9A3412")
        canvas.drawRect(0f, 0f, w.toFloat(), 180f, paint)
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("京都 観光＆宿泊プランガイド", 60f, 120f, paint)

        // Hotel Card
        val hotelRect = RectF(60f, 220f, w - 60f, 1100f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(hotelRect, 28f, 28f, paint)

        // Hotel Image Header
        val imgRect = RectF(60f, 220f, w - 60f, 650f)
        val imgGrad = LinearGradient(
            imgRect.left, imgRect.top, imgRect.right, imgRect.bottom,
            Color.parseColor("#B45309"), Color.parseColor("#D97706"),
            Shader.TileMode.CLAMP
        )
        paint.shader = imgGrad
        canvas.drawRoundRect(imgRect, 28f, 28f, paint)
        paint.shader = null

        paint.color = Color.WHITE
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("京都 祇園 雅の宿（旅館＆温泉）", w / 2f, 430f, paint)
        paint.textSize = 36f
        canvas.drawText("⭐ 4.8 / 5.0 (口コミ 850件)", w / 2f, 490f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Hotel Details
        paint.color = Color.parseColor("#1C1917")
        paint.textSize = 42f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("予約確定番号: KYOTO-2026-8891", 100f, 720f, paint)

        paint.color = Color.parseColor("#44403C")
        paint.textSize = 38f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("宿泊日程: 2026年9月12日(土) - 1泊2日 (朝夕食付き)", 100f, 780f, paint)
        canvas.drawText("チェックイン: 15:00 〜 19:00 / チェックアウト: 11:00", 100f, 840f, paint)
        canvas.drawText("所在地: 京都府京都市東山区祇園町南側570-2", 100f, 900f, paint)
        canvas.drawText("アクセス: 京阪本線 祇園四条駅より徒歩5分", 100f, 960f, paint)
        canvas.drawText("電話番号: 075-555-1234 (フロント対応 24時間)", 100f, 1020f, paint)

        // Recommended Sightseeing Spots
        val spotRect = RectF(60f, 1150f, w - 60f, 1850f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(spotRect, 28f, 28f, paint)

        paint.color = Color.parseColor("#9A3412")
        paint.textSize = 46f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("📍 宿周辺のおすすめ観光スポット", 100f, 1230f, paint)

        val spots = listOf(
            "1. 清水寺（舞台からの絶景と音羽の滝）" to "徒歩約15分 / 拝観時間 6:00 - 18:00",
            "2. 八坂神社（祇園のシンボル・縁結び）" to "徒歩約5分 / 境内自由拝観",
            "3. 花見小路通（伝統的な町家と風情ある石畳）" to "宿のすぐそば / 散策自由",
            "4. 高台寺（夜間ライトアップと枯山水庭園）" to "徒歩約10分 / 拝観料 600円"
        )

        var spotY = 1310f
        for ((name, desc) in spots) {
            paint.color = Color.parseColor("#1C1917")
            paint.textSize = 40f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(name, 100f, spotY, paint)

            paint.color = Color.parseColor("#78716C")
            paint.textSize = 34f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(desc, 100f, spotY + 45f, paint)

            spotY += 120f
        }
    }
}
