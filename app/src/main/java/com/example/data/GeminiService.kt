package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Parses a raw financial alert text (such as SMS notifications from banks)
     * into a structured JSON string.
     */
    suspend fun parseReceipt(rawText: String): ParsedTransaction? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val prompt = """
            Kamu adalah AI asisten keuangan keluarga Indonesia. Tugasmu adalah menganalisis teks notifikasi transaksi keuangan / SMS Banking berikut ini dan mengekstrak datanya ke dalam format JSON yang valid.
            
            Pilihlah salah satu Kategori yang paling sesuai dari daftar berikut:
            - "Makanan" (misal: restoran, minimarket, warung, gofood)
            - "Transportasi" (misal: bensin, ojek online, parkir, tol)
            - "Pendidikan" (misal: SPP, buku, kursus)
            - "Hiburan" (misal: bioskop, game, kafe, liburan)
            - "Lainnya" (jika tidak masuk kategori manapun)
            
            Pilihlah tipe transaksi dari:
            - "EXPENSE" (Pengeluaran / uang keluar)
            - "INCOME" (Pemasukan / uang masuk)

            Format JSON keluarannya harus persis seperti ini, tanpa penjelasan tambahan apapun:
            {
              "merchant": "Nama penerima / pengirim / toko (misal: GoPay, Cinema XXI, Pertamina)",
              "amount": Angka nominal tanpa titik atau koma (misal: 150000),
              "category": "Kategori yang dipilih",
              "type": "Tipe transaksi (EXPENSE atau INCOME)"
            }

            Teks transaksi yang ingin dianalisis:
            "$rawText"
        """.trimIndent()

        try {
            // Build JSON Body
            val requestBodyJson = JSONObject()
            
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            
            requestBodyJson.put("contents", contentsArray)

            // Setup generationConfig for JSON response
            val generationConfig = JSONObject()
            generationConfig.put("responseMimeType", "application/json")
            generationConfig.put("temperature", 0.1) // low temperature for precise factual extraction
            requestBodyJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                
                val bodyText = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(bodyText)
                val candidates = rootJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val textResponse = firstCandidate.getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Convert textResponse back into our entity
                val parsedJson = JSONObject(textResponse.trim())
                return@withContext ParsedTransaction(
                    merchant = parsedJson.optString("merchant", "Tidak Dikenal"),
                    amount = parsedJson.optDouble("amount", 0.0),
                    category = parsedJson.optString("category", "Lainnya"),
                    type = parsedJson.optString("type", "EXPENSE")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Generates a tailored investment & retirement financial advice in Indonesian.
     */
    suspend fun getFinancialAdvice(
        familyMembers: List<FamilyMember>,
        budgets: List<Budget>,
        transactions: List<Transaction>,
        investments: List<Investment>,
        retirementPlan: RetirementPlan?
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key Gemini belum diatur di AI Studio Secrets. Silakan tambahkan 'GEMINI_API_KEY' di panel Secrets untuk mengaktifkan konsultasi finansial real-time AI."
        }

        // Format family members
        val membersStr = familyMembers.joinToString("\n") { "  - ${it.name} (${it.role})" }

        // Format budgets
        val budgetsStr = budgets.joinToString("\n") { "  - Kategori ${it.category}: Anggaran Rp ${it.limitAmount.toInt()}, Terpakai Rp ${it.spentAmount.toInt()}" }

        // Format recent transactions
        val recentTx = transactions.take(10)
        val txsStr = recentTx.joinToString("\n") { "  - ${it.memberName}: ${it.type} Rp ${it.amount.toInt()} di ${it.merchant} [Kategori: ${it.category}]" }

        // Format investments
        val investmentsStr = investments.joinToString("\n") { "  - ${it.name} (${it.type}): Modal Rp ${it.amountInvested.toInt()}, Nilai Saat Ini Rp ${it.currentValue.toInt()}" }

        val retPlan = retirementPlan ?: RetirementPlan()
        val planStr = """
            - Usia Sekarang: ${retPlan.currentAge} tahun
            - Target Usia Pensiun: ${retPlan.retirementAge} tahun
            - Estimasi Kebutuhan Bulanan setelah Pensiun (nilai hari ini): Rp ${retPlan.monthlyNeedRetirement.toInt()}/bulan
            - Asumsi Laju Inflasi: ${retPlan.inflationRatePercent}% per tahun
            - Asumsi Return Investasi: ${retPlan.investmentReturnPercent}% per tahun
            - Rencana Menabung/Investasi Tahunan: Rp ${retPlan.annualSavingContribution.toInt()}/tahun
        """.trimIndent()

        val prompt = """
            Kamu adalah Konsultan Perencana Keuangan Keluarga Independen (Financial Advisor AI) terkemuka di Indonesia.
            Berikut adalah profil detail keuangan keluarga terintegrasi saat ini:

            1. ANGGOTA KELUARGA:
            $membersStr

            2. ANGGARAN BULANAN (BUDGET):
            $budgetsStr

            3. RIWAYAT TRANSAKSI TERAKHIR:
            $txsStr

            4. PORTFOLIO INVESTASI:
            $investmentsStr

            5. RENCANA PENSIUN KELUARGA:
            $planStr

            TUGAS:
            Berikan analisis keuangan, strategi alokasi investasi, dan evaluasi rencana pensiun keluarga ini secara detail, suportif, dan profesional. Tulis dalam bahasa Indonesia yang ramah, sopan, dan mudah dipahami oleh anggota keluarga. 
            
            Sajikan dalam format Markdown yang rapi dengan bagian-bagian berikut:
            - **Ringkasan Kesehatan Keuangan**: Evaluasi rasio belanja terhadap tabungan.
            - **Analisis & Optimalisasi Anggaran**: Kategori mana yang kritis atau bisa dihemat.
            - **Evaluasi Portofolio Investasi**: Apakah portofolio saat ini (Emas, Saham, Reksadana) sudah ideal atau butuh rebalancing?
            - **Kelayakan Rencana Pensiun**: Hitung secara kasar apakah rencana menabung Rp ${retPlan.annualSavingContribution.toInt()}/tahun saat ini sudah cukup untuk memenuhi kebutuhan pensiun Rp ${retPlan.monthlyNeedRetirement.toInt()}/bulan (sesuaikan inflasi), dan berikan rekomendasi aksi konkret.
        """.trimIndent()

        try {
            val requestBodyJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestBodyJson.put("contents", contentsArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Gagal terhubung ke AI Advisor. Log error: Kode respon ${response.code}"
                }
                val bodyText = response.body?.string() ?: return@withContext "Respon AI kosong."
                val rootJson = JSONObject(bodyText)
                val candidates = rootJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                return@withContext firstCandidate.getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Terjadi kesalahan koneksi AI Advisor: ${e.localizedMessage}"
        }
    }
}

data class ParsedTransaction(
    val merchant: String,
    val amount: Double,
    val category: String,
    val type: String
)
