package com.github.livingwithhippos.unchained

import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.torrentdetails.model.TorrentFileItem
import com.github.livingwithhippos.unchained.torrentdetails.model.getFilesNodes
import com.github.livingwithhippos.unchained.utilities.Node
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Test

/** This file is used to avoid errors with `gradlew lint test` */
class TestTorrentFileParser {

    private val moshi: Moshi = Moshi.Builder().build()
    private val jsonAdapter: JsonAdapter<TorrentItem> = moshi.adapter(TorrentItem::class.java)

    @Test
    fun torrentItemNodes() {
        val json =
            """
    {
	"id": "XGYA5QSAA7JI4",
	"filename": "uc-berkeley-cs61c-great-ideas-in-computer-architecture",
	"original_filename": "uc-berkeley-cs61c-great-ideas-in-computer-architecture",
	"hash": "7f53b1ae54fe80b6c98b4e263e59f5b08061000c",
	"bytes": 748852727,
	"original_bytes": 748852727,
	"host": "real-debrid.com",
	"split": 2000,
	"progress": 0,
	"status": "downloading",
	"added": "2022-10-31T10:40:30.000Z",
	"files": [
		{
			"id": 1,
			"path": "\/01-course-introduction.mp3",
			"bytes": 11440065,
			"selected": 1
		},
		{
			"id": 2,
			"path": "\/01-course-introduction.pdf",
			"bytes": 533998,
			"selected": 1
		},
		{
			"id": 3,
			"path": "\/01-course-introduction.png",
			"bytes": 60749,
			"selected": 1
		},
		{
			"id": 4,
			"path": "\/01-course-introduction_chocr.html.gz",
			"bytes": 157001,
			"selected": 1
		},
		{
			"id": 5,
			"path": "\/01-course-introduction_djvu.txt",
			"bytes": 11172,
			"selected": 1
		},
		{
			"id": 6,
			"path": "\/01-course-introduction_djvu.xml",
			"bytes": 180119,
			"selected": 1
		},
		{
			"id": 7,
			"path": "\/01-course-introduction_hocr.html",
			"bytes": 430184,
			"selected": 1
		},
		{
			"id": 8,
			"path": "\/01-course-introduction_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 9,
			"path": "\/01-course-introduction_hocr_searchtext.txt.gz",
			"bytes": 4498,
			"selected": 1
		},
		{
			"id": 10,
			"path": "\/01-course-introduction_jp2.zip",
			"bytes": 3125033,
			"selected": 1
		},
		{
			"id": 11,
			"path": "\/01-course-introduction_page_numbers.json",
			"bytes": 2491,
			"selected": 1
		},
		{
			"id": 12,
			"path": "\/01-course-introduction_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 13,
			"path": "\/01-course-introduction_spectrogram.png",
			"bytes": 265192,
			"selected": 1
		},
		{
			"id": 14,
			"path": "\/02-number-representation.mp3",
			"bytes": 11880176,
			"selected": 1
		},
		{
			"id": 15,
			"path": "\/02-number-representation.pdf",
			"bytes": 144891,
			"selected": 1
		},
		{
			"id": 16,
			"path": "\/02-number-representation.png",
			"bytes": 62304,
			"selected": 1
		},
		{
			"id": 17,
			"path": "\/02-number-representation_chocr.html.gz",
			"bytes": 149880,
			"selected": 1
		},
		{
			"id": 18,
			"path": "\/02-number-representation_djvu.txt",
			"bytes": 10707,
			"selected": 1
		},
		{
			"id": 19,
			"path": "\/02-number-representation_djvu.xml",
			"bytes": 174661,
			"selected": 1
		},
		{
			"id": 20,
			"path": "\/02-number-representation_hocr.html",
			"bytes": 396133,
			"selected": 1
		},
		{
			"id": 21,
			"path": "\/02-number-representation_hocr_pageindex.json.gz",
			"bytes": 168,
			"selected": 1
		},
		{
			"id": 22,
			"path": "\/02-number-representation_hocr_searchtext.txt.gz",
			"bytes": 3916,
			"selected": 1
		},
		{
			"id": 23,
			"path": "\/02-number-representation_jp2.zip",
			"bytes": 3107169,
			"selected": 1
		},
		{
			"id": 24,
			"path": "\/02-number-representation_page_numbers.json",
			"bytes": 2720,
			"selected": 1
		},
		{
			"id": 25,
			"path": "\/02-number-representation_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 26,
			"path": "\/02-number-representation_spectrogram.png",
			"bytes": 271329,
			"selected": 1
		},
		{
			"id": 27,
			"path": "\/03-introduction-to-c.mp3",
			"bytes": 12180062,
			"selected": 1
		},
		{
			"id": 28,
			"path": "\/03-introduction-to-c.pdf",
			"bytes": 346414,
			"selected": 1
		},
		{
			"id": 29,
			"path": "\/03-introduction-to-c.png",
			"bytes": 46951,
			"selected": 1
		},
		{
			"id": 30,
			"path": "\/03-introduction-to-c_chocr.html.gz",
			"bytes": 116092,
			"selected": 1
		},
		{
			"id": 31,
			"path": "\/03-introduction-to-c_djvu.txt",
			"bytes": 8313,
			"selected": 1
		},
		{
			"id": 32,
			"path": "\/03-introduction-to-c_djvu.xml",
			"bytes": 131261,
			"selected": 1
		},
		{
			"id": 33,
			"path": "\/03-introduction-to-c_hocr.html",
			"bytes": 279842,
			"selected": 1
		},
		{
			"id": 34,
			"path": "\/03-introduction-to-c_hocr_pageindex.json.gz",
			"bytes": 149,
			"selected": 1
		},
		{
			"id": 35,
			"path": "\/03-introduction-to-c_hocr_searchtext.txt.gz",
			"bytes": 3265,
			"selected": 1
		},
		{
			"id": 36,
			"path": "\/03-introduction-to-c_jp2.zip",
			"bytes": 2308115,
			"selected": 1
		},
		{
			"id": 37,
			"path": "\/03-introduction-to-c_page_numbers.json",
			"bytes": 2356,
			"selected": 1
		},
		{
			"id": 38,
			"path": "\/03-introduction-to-c_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 39,
			"path": "\/03-introduction-to-c_spectrogram.png",
			"bytes": 281667,
			"selected": 1
		},
		{
			"id": 40,
			"path": "\/03-notes-on-c-harvey.pdf",
			"bytes": 156441,
			"selected": 1
		},
		{
			"id": 41,
			"path": "\/03-notes-on-c-harvey_chocr.html.gz",
			"bytes": 672455,
			"selected": 1
		},
		{
			"id": 42,
			"path": "\/03-notes-on-c-harvey_djvu.txt",
			"bytes": 53263,
			"selected": 1
		},
		{
			"id": 43,
			"path": "\/03-notes-on-c-harvey_djvu.xml",
			"bytes": 697183,
			"selected": 1
		},
		{
			"id": 44,
			"path": "\/03-notes-on-c-harvey_hocr.html",
			"bytes": 1274846,
			"selected": 1
		},
		{
			"id": 45,
			"path": "\/03-notes-on-c-harvey_hocr_pageindex.json.gz",
			"bytes": 247,
			"selected": 1
		},
		{
			"id": 46,
			"path": "\/03-notes-on-c-harvey_hocr_searchtext.txt.gz",
			"bytes": 19346,
			"selected": 1
		},
		{
			"id": 47,
			"path": "\/03-notes-on-c-harvey_jp2.zip",
			"bytes": 10678603,
			"selected": 1
		},
		{
			"id": 48,
			"path": "\/03-notes-on-c-harvey_page_numbers.json",
			"bytes": 3815,
			"selected": 1
		},
		{
			"id": 49,
			"path": "\/03-notes-on-c-harvey_scandata.xml",
			"bytes": 6940,
			"selected": 1
		},
		{
			"id": 50,
			"path": "\/04-c-pointers-and-arrays.mp3",
			"bytes": 10820127,
			"selected": 1
		},
		{
			"id": 51,
			"path": "\/04-c-pointers-and-arrays.pdf",
			"bytes": 140495,
			"selected": 1
		},
		{
			"id": 52,
			"path": "\/04-c-pointers-and-arrays.png",
			"bytes": 45776,
			"selected": 1
		},
		{
			"id": 53,
			"path": "\/04-c-pointers-and-arrays_chocr.html.gz",
			"bytes": 128855,
			"selected": 1
		},
		{
			"id": 54,
			"path": "\/04-c-pointers-and-arrays_djvu.txt",
			"bytes": 9240,
			"selected": 1
		},
		{
			"id": 55,
			"path": "\/04-c-pointers-and-arrays_djvu.xml",
			"bytes": 142829,
			"selected": 1
		},
		{
			"id": 56,
			"path": "\/04-c-pointers-and-arrays_hocr.html",
			"bytes": 297646,
			"selected": 1
		},
		{
			"id": 57,
			"path": "\/04-c-pointers-and-arrays_hocr_pageindex.json.gz",
			"bytes": 150,
			"selected": 1
		},
		{
			"id": 58,
			"path": "\/04-c-pointers-and-arrays_hocr_searchtext.txt.gz",
			"bytes": 3541,
			"selected": 1
		},
		{
			"id": 59,
			"path": "\/04-c-pointers-and-arrays_jp2.zip",
			"bytes": 2555701,
			"selected": 1
		},
		{
			"id": 60,
			"path": "\/04-c-pointers-and-arrays_page_numbers.json",
			"bytes": 2165,
			"selected": 1
		},
		{
			"id": 61,
			"path": "\/04-c-pointers-and-arrays_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 62,
			"path": "\/04-c-pointers-and-arrays_spectrogram.png",
			"bytes": 292152,
			"selected": 1
		},
		{
			"id": 63,
			"path": "\/05-c-structs-and-memory-management.mp3",
			"bytes": 11400046,
			"selected": 1
		},
		{
			"id": 64,
			"path": "\/05-c-structs-and-memory-management.pdf",
			"bytes": 146981,
			"selected": 1
		},
		{
			"id": 65,
			"path": "\/05-c-structs-and-memory-management.png",
			"bytes": 45782,
			"selected": 1
		},
		{
			"id": 66,
			"path": "\/05-c-structs-and-memory-management_chocr.html.gz",
			"bytes": 132613,
			"selected": 1
		},
		{
			"id": 67,
			"path": "\/05-c-structs-and-memory-management_djvu.txt",
			"bytes": 9414,
			"selected": 1
		},
		{
			"id": 68,
			"path": "\/05-c-structs-and-memory-management_djvu.xml",
			"bytes": 158585,
			"selected": 1
		},
		{
			"id": 69,
			"path": "\/05-c-structs-and-memory-management_hocr.html",
			"bytes": 356434,
			"selected": 1
		},
		{
			"id": 70,
			"path": "\/05-c-structs-and-memory-management_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 71,
			"path": "\/05-c-structs-and-memory-management_hocr_searchtext.txt.gz",
			"bytes": 2916,
			"selected": 1
		},
		{
			"id": 72,
			"path": "\/05-c-structs-and-memory-management_jp2.zip",
			"bytes": 2405130,
			"selected": 1
		},
		{
			"id": 73,
			"path": "\/05-c-structs-and-memory-management_page_numbers.json",
			"bytes": 2439,
			"selected": 1
		},
		{
			"id": 74,
			"path": "\/05-c-structs-and-memory-management_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 75,
			"path": "\/05-c-structs-and-memory-management_spectrogram.png",
			"bytes": 285901,
			"selected": 1
		},
		{
			"id": 76,
			"path": "\/06-c-memory-management-1.pdf",
			"bytes": 176254,
			"selected": 1
		},
		{
			"id": 77,
			"path": "\/06-c-memory-management-1_chocr.html.gz",
			"bytes": 173944,
			"selected": 1
		},
		{
			"id": 78,
			"path": "\/06-c-memory-management-1_djvu.txt",
			"bytes": 12576,
			"selected": 1
		},
		{
			"id": 79,
			"path": "\/06-c-memory-management-1_djvu.xml",
			"bytes": 193722,
			"selected": 1
		},
		{
			"id": 80,
			"path": "\/06-c-memory-management-1_hocr.html",
			"bytes": 414609,
			"selected": 1
		},
		{
			"id": 81,
			"path": "\/06-c-memory-management-1_hocr_pageindex.json.gz",
			"bytes": 193,
			"selected": 1
		},
		{
			"id": 82,
			"path": "\/06-c-memory-management-1_hocr_searchtext.txt.gz",
			"bytes": 4181,
			"selected": 1
		},
		{
			"id": 83,
			"path": "\/06-c-memory-management-1_jp2.zip",
			"bytes": 3682929,
			"selected": 1
		},
		{
			"id": 84,
			"path": "\/06-c-memory-management-1_page_numbers.json",
			"bytes": 3100,
			"selected": 1
		},
		{
			"id": 85,
			"path": "\/06-c-memory-management-1_scandata.xml",
			"bytes": 5056,
			"selected": 1
		},
		{
			"id": 86,
			"path": "\/06-memory-management-1.mp3",
			"bytes": 11920091,
			"selected": 1
		},
		{
			"id": 87,
			"path": "\/06-memory-management-1.png",
			"bytes": 47143,
			"selected": 1
		},
		{
			"id": 88,
			"path": "\/06-memory-management-1_spectrogram.png",
			"bytes": 282335,
			"selected": 1
		},
		{
			"id": 89,
			"path": "\/07-c-memory-management-2.pdf",
			"bytes": 176254,
			"selected": 1
		},
		{
			"id": 90,
			"path": "\/07-c-memory-management-2_chocr.html.gz",
			"bytes": 173943,
			"selected": 1
		},
		{
			"id": 91,
			"path": "\/07-c-memory-management-2_djvu.txt",
			"bytes": 12576,
			"selected": 1
		},
		{
			"id": 92,
			"path": "\/07-c-memory-management-2_djvu.xml",
			"bytes": 193722,
			"selected": 1
		},
		{
			"id": 93,
			"path": "\/07-c-memory-management-2_hocr.html",
			"bytes": 414609,
			"selected": 1
		},
		{
			"id": 94,
			"path": "\/07-c-memory-management-2_hocr_pageindex.json.gz",
			"bytes": 193,
			"selected": 1
		},
		{
			"id": 95,
			"path": "\/07-c-memory-management-2_hocr_searchtext.txt.gz",
			"bytes": 4181,
			"selected": 1
		},
		{
			"id": 96,
			"path": "\/07-c-memory-management-2_jp2.zip",
			"bytes": 3682929,
			"selected": 1
		},
		{
			"id": 97,
			"path": "\/07-c-memory-management-2_page_numbers.json",
			"bytes": 3100,
			"selected": 1
		},
		{
			"id": 98,
			"path": "\/07-c-memory-management-2_scandata.xml",
			"bytes": 5056,
			"selected": 1
		},
		{
			"id": 99,
			"path": "\/07-hilfinger-notes.pdf",
			"bytes": 228213,
			"selected": 1
		},
		{
			"id": 100,
			"path": "\/07-hilfinger-notes_chocr.html.gz",
			"bytes": 628453,
			"selected": 1
		},
		{
			"id": 101,
			"path": "\/07-hilfinger-notes_djvu.txt",
			"bytes": 47905,
			"selected": 1
		},
		{
			"id": 102,
			"path": "\/07-hilfinger-notes_djvu.xml",
			"bytes": 636426,
			"selected": 1
		},
		{
			"id": 103,
			"path": "\/07-hilfinger-notes_hocr.html",
			"bytes": 1227887,
			"selected": 1
		},
		{
			"id": 104,
			"path": "\/07-hilfinger-notes_hocr_pageindex.json.gz",
			"bytes": 289,
			"selected": 1
		},
		{
			"id": 105,
			"path": "\/07-hilfinger-notes_hocr_searchtext.txt.gz",
			"bytes": 16280,
			"selected": 1
		},
		{
			"id": 106,
			"path": "\/07-hilfinger-notes_jp2.zip",
			"bytes": 8941426,
			"selected": 1
		},
		{
			"id": 107,
			"path": "\/07-hilfinger-notes_page_numbers.json",
			"bytes": 4805,
			"selected": 1
		},
		{
			"id": 108,
			"path": "\/07-hilfinger-notes_scandata.xml",
			"bytes": 8386,
			"selected": 1
		},
		{
			"id": 109,
			"path": "\/07-memory-management-2.mp3",
			"bytes": 12020088,
			"selected": 1
		},
		{
			"id": 110,
			"path": "\/07-memory-management-2.png",
			"bytes": 55803,
			"selected": 1
		},
		{
			"id": 111,
			"path": "\/07-memory-management-2_spectrogram.png",
			"bytes": 276609,
			"selected": 1
		},
		{
			"id": 112,
			"path": "\/08-introduction-to-mips.mp3",
			"bytes": 11272045,
			"selected": 1
		},
		{
			"id": 113,
			"path": "\/08-introduction-to-mips.pdf",
			"bytes": 370451,
			"selected": 1
		},
		{
			"id": 114,
			"path": "\/08-introduction-to-mips.png",
			"bytes": 58576,
			"selected": 1
		},
		{
			"id": 115,
			"path": "\/08-introduction-to-mips_chocr.html.gz",
			"bytes": 154612,
			"selected": 1
		},
		{
			"id": 116,
			"path": "\/08-introduction-to-mips_djvu.txt",
			"bytes": 11065,
			"selected": 1
		},
		{
			"id": 117,
			"path": "\/08-introduction-to-mips_djvu.xml",
			"bytes": 166083,
			"selected": 1
		},
		{
			"id": 118,
			"path": "\/08-introduction-to-mips_hocr.html",
			"bytes": 355239,
			"selected": 1
		},
		{
			"id": 119,
			"path": "\/08-introduction-to-mips_hocr_pageindex.json.gz",
			"bytes": 159,
			"selected": 1
		},
		{
			"id": 120,
			"path": "\/08-introduction-to-mips_hocr_searchtext.txt.gz",
			"bytes": 4033,
			"selected": 1
		},
		{
			"id": 121,
			"path": "\/08-introduction-to-mips_jp2.zip",
			"bytes": 3011014,
			"selected": 1
		},
		{
			"id": 122,
			"path": "\/08-introduction-to-mips_page_numbers.json",
			"bytes": 2367,
			"selected": 1
		},
		{
			"id": 123,
			"path": "\/08-introduction-to-mips_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 124,
			"path": "\/08-introduction-to-mips_spectrogram.png",
			"bytes": 273090,
			"selected": 1
		},
		{
			"id": 125,
			"path": "\/09-mips-load-store-and-branch-instructions-1.mp3",
			"bytes": 11200157,
			"selected": 1
		},
		{
			"id": 126,
			"path": "\/09-mips-load-store-and-branch-instructions-1.pdf",
			"bytes": 173997,
			"selected": 1
		},
		{
			"id": 127,
			"path": "\/09-mips-load-store-and-branch-instructions-1.png",
			"bytes": 55009,
			"selected": 1
		},
		{
			"id": 128,
			"path": "\/09-mips-load-store-and-branch-instructions-1_chocr.html.gz",
			"bytes": 140652,
			"selected": 1
		},
		{
			"id": 129,
			"path": "\/09-mips-load-store-and-branch-instructions-1_djvu.txt",
			"bytes": 10021,
			"selected": 1
		},
		{
			"id": 130,
			"path": "\/09-mips-load-store-and-branch-instructions-1_djvu.xml",
			"bytes": 150532,
			"selected": 1
		},
		{
			"id": 131,
			"path": "\/09-mips-load-store-and-branch-instructions-1_hocr.html",
			"bytes": 311528,
			"selected": 1
		},
		{
			"id": 132,
			"path": "\/09-mips-load-store-and-branch-instructions-1_hocr_pageindex.json.gz",
			"bytes": 149,
			"selected": 1
		},
		{
			"id": 133,
			"path": "\/09-mips-load-store-and-branch-instructions-1_hocr_searchtext.txt.gz",
			"bytes": 3520,
			"selected": 1
		},
		{
			"id": 134,
			"path": "\/09-mips-load-store-and-branch-instructions-1_jp2.zip",
			"bytes": 2921106,
			"selected": 1
		},
		{
			"id": 135,
			"path": "\/09-mips-load-store-and-branch-instructions-1_page_numbers.json",
			"bytes": 2294,
			"selected": 1
		},
		{
			"id": 136,
			"path": "\/09-mips-load-store-and-branch-instructions-1_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 137,
			"path": "\/09-mips-load-store-and-branch-instructions-1_spectrogram.png",
			"bytes": 280568,
			"selected": 1
		},
		{
			"id": 138,
			"path": "\/10-mips-branch-instructions-2.mp3",
			"bytes": 12060107,
			"selected": 1
		},
		{
			"id": 139,
			"path": "\/10-mips-branch-instructions-2.pdf",
			"bytes": 189326,
			"selected": 1
		},
		{
			"id": 140,
			"path": "\/10-mips-branch-instructions-2.png",
			"bytes": 48816,
			"selected": 1
		},
		{
			"id": 141,
			"path": "\/10-mips-branch-instructions-2_chocr.html.gz",
			"bytes": 130492,
			"selected": 1
		},
		{
			"id": 142,
			"path": "\/10-mips-branch-instructions-2_djvu.txt",
			"bytes": 9199,
			"selected": 1
		},
		{
			"id": 143,
			"path": "\/10-mips-branch-instructions-2_djvu.xml",
			"bytes": 144727,
			"selected": 1
		},
		{
			"id": 144,
			"path": "\/10-mips-branch-instructions-2_hocr.html",
			"bytes": 304203,
			"selected": 1
		},
		{
			"id": 145,
			"path": "\/10-mips-branch-instructions-2_hocr_pageindex.json.gz",
			"bytes": 161,
			"selected": 1
		},
		{
			"id": 146,
			"path": "\/10-mips-branch-instructions-2_hocr_searchtext.txt.gz",
			"bytes": 3421,
			"selected": 1
		},
		{
			"id": 147,
			"path": "\/10-mips-branch-instructions-2_jp2.zip",
			"bytes": 2819387,
			"selected": 1
		},
		{
			"id": 148,
			"path": "\/10-mips-branch-instructions-2_page_numbers.json",
			"bytes": 2405,
			"selected": 1
		},
		{
			"id": 149,
			"path": "\/10-mips-branch-instructions-2_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 150,
			"path": "\/10-mips-branch-instructions-2_spectrogram.png",
			"bytes": 282412,
			"selected": 1
		},
		{
			"id": 151,
			"path": "\/11-mips-procedures-1.mp3",
			"bytes": 10760150,
			"selected": 1
		},
		{
			"id": 152,
			"path": "\/11-mips-procedures-1.pdf",
			"bytes": 155337,
			"selected": 1
		},
		{
			"id": 153,
			"path": "\/11-mips-procedures-1.png",
			"bytes": 46061,
			"selected": 1
		},
		{
			"id": 154,
			"path": "\/11-mips-procedures-1_chocr.html.gz",
			"bytes": 115340,
			"selected": 1
		},
		{
			"id": 155,
			"path": "\/11-mips-procedures-1_djvu.txt",
			"bytes": 8108,
			"selected": 1
		},
		{
			"id": 156,
			"path": "\/11-mips-procedures-1_djvu.xml",
			"bytes": 125435,
			"selected": 1
		},
		{
			"id": 157,
			"path": "\/11-mips-procedures-1_hocr.html",
			"bytes": 265512,
			"selected": 1
		},
		{
			"id": 158,
			"path": "\/11-mips-procedures-1_hocr_pageindex.json.gz",
			"bytes": 149,
			"selected": 1
		},
		{
			"id": 159,
			"path": "\/11-mips-procedures-1_hocr_searchtext.txt.gz",
			"bytes": 2976,
			"selected": 1
		},
		{
			"id": 160,
			"path": "\/11-mips-procedures-1_jp2.zip",
			"bytes": 2513315,
			"selected": 1
		},
		{
			"id": 161,
			"path": "\/11-mips-procedures-1_page_numbers.json",
			"bytes": 2180,
			"selected": 1
		},
		{
			"id": 162,
			"path": "\/11-mips-procedures-1_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 163,
			"path": "\/11-mips-procedures-1_spectrogram.png",
			"bytes": 278796,
			"selected": 1
		},
		{
			"id": 164,
			"path": "\/12-mips-procedures-2-and-logical-ops.mp3",
			"bytes": 11736085,
			"selected": 1
		},
		{
			"id": 165,
			"path": "\/12-mips-procedures-2-and-logical-ops.pdf",
			"bytes": 174990,
			"selected": 1
		},
		{
			"id": 166,
			"path": "\/12-mips-procedures-2-and-logical-ops.png",
			"bytes": 45855,
			"selected": 1
		},
		{
			"id": 167,
			"path": "\/12-mips-procedures-2-and-logical-ops_chocr.html.gz",
			"bytes": 192437,
			"selected": 1
		},
		{
			"id": 168,
			"path": "\/12-mips-procedures-2-and-logical-ops_djvu.txt",
			"bytes": 13874,
			"selected": 1
		},
		{
			"id": 169,
			"path": "\/12-mips-procedures-2-and-logical-ops_djvu.xml",
			"bytes": 216029,
			"selected": 1
		},
		{
			"id": 170,
			"path": "\/12-mips-procedures-2-and-logical-ops_hocr.html",
			"bytes": 440053,
			"selected": 1
		},
		{
			"id": 171,
			"path": "\/12-mips-procedures-2-and-logical-ops_hocr_pageindex.json.gz",
			"bytes": 202,
			"selected": 1
		},
		{
			"id": 172,
			"path": "\/12-mips-procedures-2-and-logical-ops_hocr_searchtext.txt.gz",
			"bytes": 4430,
			"selected": 1
		},
		{
			"id": 173,
			"path": "\/12-mips-procedures-2-and-logical-ops_jp2.zip",
			"bytes": 3851680,
			"selected": 1
		},
		{
			"id": 174,
			"path": "\/12-mips-procedures-2-and-logical-ops_page_numbers.json",
			"bytes": 3087,
			"selected": 1
		},
		{
			"id": 175,
			"path": "\/12-mips-procedures-2-and-logical-ops_scandata.xml",
			"bytes": 5376,
			"selected": 1
		},
		{
			"id": 176,
			"path": "\/12-mips-procedures-2-and-logical-ops_spectrogram.png",
			"bytes": 282876,
			"selected": 1
		},
		{
			"id": 177,
			"path": "\/13-mips-instruction-representation-1.mp3",
			"bytes": 11896164,
			"selected": 1
		},
		{
			"id": 178,
			"path": "\/13-mips-instruction-representation-1.pdf",
			"bytes": 154599,
			"selected": 1
		},
		{
			"id": 179,
			"path": "\/13-mips-instruction-representation-1.png",
			"bytes": 46806,
			"selected": 1
		},
		{
			"id": 180,
			"path": "\/13-mips-instruction-representation-1_chocr.html.gz",
			"bytes": 155594,
			"selected": 1
		},
		{
			"id": 181,
			"path": "\/13-mips-instruction-representation-1_djvu.txt",
			"bytes": 11269,
			"selected": 1
		},
		{
			"id": 182,
			"path": "\/13-mips-instruction-representation-1_djvu.xml",
			"bytes": 162752,
			"selected": 1
		},
		{
			"id": 183,
			"path": "\/13-mips-instruction-representation-1_hocr.html",
			"bytes": 334673,
			"selected": 1
		},
		{
			"id": 184,
			"path": "\/13-mips-instruction-representation-1_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 185,
			"path": "\/13-mips-instruction-representation-1_hocr_searchtext.txt.gz",
			"bytes": 3871,
			"selected": 1
		},
		{
			"id": 186,
			"path": "\/13-mips-instruction-representation-1_jp2.zip",
			"bytes": 3068814,
			"selected": 1
		},
		{
			"id": 187,
			"path": "\/13-mips-instruction-representation-1_page_numbers.json",
			"bytes": 2328,
			"selected": 1
		},
		{
			"id": 188,
			"path": "\/13-mips-instruction-representation-1_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 189,
			"path": "\/13-mips-instruction-representation-1_spectrogram.png",
			"bytes": 277415,
			"selected": 1
		},
		{
			"id": 190,
			"path": "\/14-mips-instruction-representation-2.mp3",
			"bytes": 11652075,
			"selected": 1
		},
		{
			"id": 191,
			"path": "\/14-mips-instruction-representation-2.pdf",
			"bytes": 134987,
			"selected": 1
		},
		{
			"id": 192,
			"path": "\/14-mips-instruction-representation-2.png",
			"bytes": 27689,
			"selected": 1
		},
		{
			"id": 193,
			"path": "\/14-mips-instruction-representation-2_chocr.html.gz",
			"bytes": 128551,
			"selected": 1
		},
		{
			"id": 194,
			"path": "\/14-mips-instruction-representation-2_djvu.txt",
			"bytes": 9141,
			"selected": 1
		},
		{
			"id": 195,
			"path": "\/14-mips-instruction-representation-2_djvu.xml",
			"bytes": 142622,
			"selected": 1
		},
		{
			"id": 196,
			"path": "\/14-mips-instruction-representation-2_hocr.html",
			"bytes": 313611,
			"selected": 1
		},
		{
			"id": 197,
			"path": "\/14-mips-instruction-representation-2_hocr_pageindex.json.gz",
			"bytes": 152,
			"selected": 1
		},
		{
			"id": 198,
			"path": "\/14-mips-instruction-representation-2_hocr_searchtext.txt.gz",
			"bytes": 3080,
			"selected": 1
		},
		{
			"id": 199,
			"path": "\/14-mips-instruction-representation-2_jp2.zip",
			"bytes": 2543066,
			"selected": 1
		},
		{
			"id": 200,
			"path": "\/14-mips-instruction-representation-2_page_numbers.json",
			"bytes": 2123,
			"selected": 1
		},
		{
			"id": 201,
			"path": "\/14-mips-instruction-representation-2_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 202,
			"path": "\/14-mips-instruction-representation-2_spectrogram.png",
			"bytes": 140386,
			"selected": 1
		},
		{
			"id": 203,
			"path": "\/15-floating-point-1.mp3",
			"bytes": 11932108,
			"selected": 1
		},
		{
			"id": 204,
			"path": "\/15-floating-point-1.pdf",
			"bytes": 138859,
			"selected": 1
		},
		{
			"id": 205,
			"path": "\/15-floating-point-1.png",
			"bytes": 63749,
			"selected": 1
		},
		{
			"id": 206,
			"path": "\/15-floating-point-1_chocr.html.gz",
			"bytes": 153574,
			"selected": 1
		},
		{
			"id": 207,
			"path": "\/15-floating-point-1_djvu.txt",
			"bytes": 11093,
			"selected": 1
		},
		{
			"id": 208,
			"path": "\/15-floating-point-1_djvu.xml",
			"bytes": 162527,
			"selected": 1
		},
		{
			"id": 209,
			"path": "\/15-floating-point-1_hocr.html",
			"bytes": 339265,
			"selected": 1
		},
		{
			"id": 210,
			"path": "\/15-floating-point-1_hocr_pageindex.json.gz",
			"bytes": 172,
			"selected": 1
		},
		{
			"id": 211,
			"path": "\/15-floating-point-1_hocr_searchtext.txt.gz",
			"bytes": 4235,
			"selected": 1
		},
		{
			"id": 212,
			"path": "\/15-floating-point-1_jp2.zip",
			"bytes": 3144656,
			"selected": 1
		},
		{
			"id": 213,
			"path": "\/15-floating-point-1_page_numbers.json",
			"bytes": 2635,
			"selected": 1
		},
		{
			"id": 214,
			"path": "\/15-floating-point-1_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 215,
			"path": "\/15-floating-point-1_spectrogram.png",
			"bytes": 272107,
			"selected": 1
		},
		{
			"id": 216,
			"path": "\/16-floating-point-2.mp3",
			"bytes": 12052062,
			"selected": 1
		},
		{
			"id": 217,
			"path": "\/16-floating-point-2.pdf",
			"bytes": 179447,
			"selected": 1
		},
		{
			"id": 218,
			"path": "\/16-floating-point-2.png",
			"bytes": 47084,
			"selected": 1
		},
		{
			"id": 219,
			"path": "\/16-floating-point-2_chocr.html.gz",
			"bytes": 149565,
			"selected": 1
		},
		{
			"id": 220,
			"path": "\/16-floating-point-2_djvu.txt",
			"bytes": 10716,
			"selected": 1
		},
		{
			"id": 221,
			"path": "\/16-floating-point-2_djvu.xml",
			"bytes": 160540,
			"selected": 1
		},
		{
			"id": 222,
			"path": "\/16-floating-point-2_hocr.html",
			"bytes": 342927,
			"selected": 1
		},
		{
			"id": 223,
			"path": "\/16-floating-point-2_hocr_pageindex.json.gz",
			"bytes": 168,
			"selected": 1
		},
		{
			"id": 224,
			"path": "\/16-floating-point-2_hocr_searchtext.txt.gz",
			"bytes": 3989,
			"selected": 1
		},
		{
			"id": 225,
			"path": "\/16-floating-point-2_jp2.zip",
			"bytes": 3086132,
			"selected": 1
		},
		{
			"id": 226,
			"path": "\/16-floating-point-2_page_numbers.json",
			"bytes": 2677,
			"selected": 1
		},
		{
			"id": 227,
			"path": "\/16-floating-point-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 228,
			"path": "\/16-floating-point-2_spectrogram.png",
			"bytes": 280689,
			"selected": 1
		},
		{
			"id": 229,
			"path": "\/17-mips-instruction-representation-3.mp3",
			"bytes": 10464131,
			"selected": 1
		},
		{
			"id": 230,
			"path": "\/17-mips-instruction-representation-3.pdf",
			"bytes": 147749,
			"selected": 1
		},
		{
			"id": 231,
			"path": "\/17-mips-instruction-representation-3.png",
			"bytes": 47974,
			"selected": 1
		},
		{
			"id": 232,
			"path": "\/17-mips-instruction-representation-3_chocr.html.gz",
			"bytes": 154506,
			"selected": 1
		},
		{
			"id": 233,
			"path": "\/17-mips-instruction-representation-3_djvu.txt",
			"bytes": 11021,
			"selected": 1
		},
		{
			"id": 234,
			"path": "\/17-mips-instruction-representation-3_djvu.xml",
			"bytes": 159486,
			"selected": 1
		},
		{
			"id": 235,
			"path": "\/17-mips-instruction-representation-3_hocr.html",
			"bytes": 353572,
			"selected": 1
		},
		{
			"id": 236,
			"path": "\/17-mips-instruction-representation-3_hocr_pageindex.json.gz",
			"bytes": 182,
			"selected": 1
		},
		{
			"id": 237,
			"path": "\/17-mips-instruction-representation-3_hocr_searchtext.txt.gz",
			"bytes": 3615,
			"selected": 1
		},
		{
			"id": 238,
			"path": "\/17-mips-instruction-representation-3_jp2.zip",
			"bytes": 3151958,
			"selected": 1
		},
		{
			"id": 239,
			"path": "\/17-mips-instruction-representation-3_page_numbers.json",
			"bytes": 2914,
			"selected": 1
		},
		{
			"id": 240,
			"path": "\/17-mips-instruction-representation-3_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 241,
			"path": "\/17-mips-instruction-representation-3_spectrogram.png",
			"bytes": 284554,
			"selected": 1
		},
		{
			"id": 242,
			"path": "\/18-compilation-assembly-linking-1.mp3",
			"bytes": 10912078,
			"selected": 1
		},
		{
			"id": 243,
			"path": "\/18-compilation-assembly-linking-1.pdf",
			"bytes": 152998,
			"selected": 1
		},
		{
			"id": 244,
			"path": "\/18-compilation-assembly-linking-1.png",
			"bytes": 46914,
			"selected": 1
		},
		{
			"id": 245,
			"path": "\/18-compilation-assembly-linking-1_chocr.html.gz",
			"bytes": 140596,
			"selected": 1
		},
		{
			"id": 246,
			"path": "\/18-compilation-assembly-linking-1_djvu.txt",
			"bytes": 10074,
			"selected": 1
		},
		{
			"id": 247,
			"path": "\/18-compilation-assembly-linking-1_djvu.xml",
			"bytes": 147878,
			"selected": 1
		},
		{
			"id": 248,
			"path": "\/18-compilation-assembly-linking-1_hocr.html",
			"bytes": 312826,
			"selected": 1
		},
		{
			"id": 249,
			"path": "\/18-compilation-assembly-linking-1_hocr_pageindex.json.gz",
			"bytes": 171,
			"selected": 1
		},
		{
			"id": 250,
			"path": "\/18-compilation-assembly-linking-1_hocr_searchtext.txt.gz",
			"bytes": 3681,
			"selected": 1
		},
		{
			"id": 251,
			"path": "\/18-compilation-assembly-linking-1_jp2.zip",
			"bytes": 3008609,
			"selected": 1
		},
		{
			"id": 252,
			"path": "\/18-compilation-assembly-linking-1_page_numbers.json",
			"bytes": 2586,
			"selected": 1
		},
		{
			"id": 253,
			"path": "\/18-compilation-assembly-linking-1_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 254,
			"path": "\/18-compilation-assembly-linking-1_spectrogram.png",
			"bytes": 284690,
			"selected": 1
		},
		{
			"id": 255,
			"path": "\/19-compilation-assembly-linking-2.mp3",
			"bytes": 11972127,
			"selected": 1
		},
		{
			"id": 256,
			"path": "\/19-compilation-assembly-linking-2.pdf",
			"bytes": 188893,
			"selected": 1
		},
		{
			"id": 257,
			"path": "\/19-compilation-assembly-linking-2.png",
			"bytes": 47278,
			"selected": 1
		},
		{
			"id": 258,
			"path": "\/19-compilation-assembly-linking-2_chocr.html.gz",
			"bytes": 203397,
			"selected": 1
		},
		{
			"id": 259,
			"path": "\/19-compilation-assembly-linking-2_djvu.txt",
			"bytes": 15056,
			"selected": 1
		},
		{
			"id": 260,
			"path": "\/19-compilation-assembly-linking-2_djvu.xml",
			"bytes": 221423,
			"selected": 1
		},
		{
			"id": 261,
			"path": "\/19-compilation-assembly-linking-2_hocr.html",
			"bytes": 478838,
			"selected": 1
		},
		{
			"id": 262,
			"path": "\/19-compilation-assembly-linking-2_hocr_pageindex.json.gz",
			"bytes": 211,
			"selected": 1
		},
		{
			"id": 263,
			"path": "\/19-compilation-assembly-linking-2_hocr_searchtext.txt.gz",
			"bytes": 5188,
			"selected": 1
		},
		{
			"id": 264,
			"path": "\/19-compilation-assembly-linking-2_jp2.zip",
			"bytes": 4371369,
			"selected": 1
		},
		{
			"id": 265,
			"path": "\/19-compilation-assembly-linking-2_page_numbers.json",
			"bytes": 3420,
			"selected": 1
		},
		{
			"id": 266,
			"path": "\/19-compilation-assembly-linking-2_scandata.xml",
			"bytes": 5696,
			"selected": 1
		},
		{
			"id": 267,
			"path": "\/19-compilation-assembly-linking-2_spectrogram.png",
			"bytes": 278569,
			"selected": 1
		},
		{
			"id": 268,
			"path": "\/20-introduction-to-synchronous-digital-systems.mp3",
			"bytes": 12172121,
			"selected": 1
		},
		{
			"id": 269,
			"path": "\/20-introduction-to-synchronous-digital-systems.pdf",
			"bytes": 2679520,
			"selected": 1
		},
		{
			"id": 270,
			"path": "\/20-introduction-to-synchronous-digital-systems.png",
			"bytes": 47523,
			"selected": 1
		},
		{
			"id": 271,
			"path": "\/20-introduction-to-synchronous-digital-systems_chocr.html.gz",
			"bytes": 65973,
			"selected": 1
		},
		{
			"id": 272,
			"path": "\/20-introduction-to-synchronous-digital-systems_djvu.txt",
			"bytes": 4676,
			"selected": 1
		},
		{
			"id": 273,
			"path": "\/20-introduction-to-synchronous-digital-systems_djvu.xml",
			"bytes": 74142,
			"selected": 1
		},
		{
			"id": 274,
			"path": "\/20-introduction-to-synchronous-digital-systems_hocr.html",
			"bytes": 161692,
			"selected": 1
		},
		{
			"id": 275,
			"path": "\/20-introduction-to-synchronous-digital-systems_hocr_pageindex.json.gz",
			"bytes": 113,
			"selected": 1
		},
		{
			"id": 276,
			"path": "\/20-introduction-to-synchronous-digital-systems_hocr_searchtext.txt.gz",
			"bytes": 1898,
			"selected": 1
		},
		{
			"id": 277,
			"path": "\/20-introduction-to-synchronous-digital-systems_jp2.zip",
			"bytes": 1495323,
			"selected": 1
		},
		{
			"id": 278,
			"path": "\/20-introduction-to-synchronous-digital-systems_page_numbers.json",
			"bytes": 1447,
			"selected": 1
		},
		{
			"id": 279,
			"path": "\/20-introduction-to-synchronous-digital-systems_scandata.xml",
			"bytes": 2499,
			"selected": 1
		},
		{
			"id": 280,
			"path": "\/20-introduction-to-synchronous-digital-systems_spectrogram.png",
			"bytes": 279328,
			"selected": 1
		},
		{
			"id": 281,
			"path": "\/21-state-elements.mp3",
			"bytes": 11892088,
			"selected": 1
		},
		{
			"id": 282,
			"path": "\/21-state-elements.pdf",
			"bytes": 1877963,
			"selected": 1
		},
		{
			"id": 283,
			"path": "\/21-state-elements.png",
			"bytes": 45450,
			"selected": 1
		},
		{
			"id": 284,
			"path": "\/21-state-elements_chocr.html.gz",
			"bytes": 107863,
			"selected": 1
		},
		{
			"id": 285,
			"path": "\/21-state-elements_djvu.txt",
			"bytes": 7641,
			"selected": 1
		},
		{
			"id": 286,
			"path": "\/21-state-elements_djvu.xml",
			"bytes": 121193,
			"selected": 1
		},
		{
			"id": 287,
			"path": "\/21-state-elements_hocr.html",
			"bytes": 259942,
			"selected": 1
		},
		{
			"id": 288,
			"path": "\/21-state-elements_hocr_pageindex.json.gz",
			"bytes": 143,
			"selected": 1
		},
		{
			"id": 289,
			"path": "\/21-state-elements_hocr_searchtext.txt.gz",
			"bytes": 2810,
			"selected": 1
		},
		{
			"id": 290,
			"path": "\/21-state-elements_jp2.zip",
			"bytes": 2307995,
			"selected": 1
		},
		{
			"id": 291,
			"path": "\/21-state-elements_page_numbers.json",
			"bytes": 1998,
			"selected": 1
		},
		{
			"id": 292,
			"path": "\/21-state-elements_scandata.xml",
			"bytes": 3456,
			"selected": 1
		},
		{
			"id": 293,
			"path": "\/21-state-elements_spectrogram.png",
			"bytes": 281151,
			"selected": 1
		},
		{
			"id": 294,
			"path": "\/22-boolean-logic.pdf",
			"bytes": 361367,
			"selected": 1
		},
		{
			"id": 295,
			"path": "\/22-boolean-logic_chocr.html.gz",
			"bytes": 184873,
			"selected": 1
		},
		{
			"id": 296,
			"path": "\/22-boolean-logic_djvu.txt",
			"bytes": 14121,
			"selected": 1
		},
		{
			"id": 297,
			"path": "\/22-boolean-logic_djvu.xml",
			"bytes": 201107,
			"selected": 1
		},
		{
			"id": 298,
			"path": "\/22-boolean-logic_hocr.html",
			"bytes": 394012,
			"selected": 1
		},
		{
			"id": 299,
			"path": "\/22-boolean-logic_hocr_pageindex.json.gz",
			"bytes": 156,
			"selected": 1
		},
		{
			"id": 300,
			"path": "\/22-boolean-logic_hocr_searchtext.txt.gz",
			"bytes": 5221,
			"selected": 1
		},
		{
			"id": 301,
			"path": "\/22-boolean-logic_jp2.zip",
			"bytes": 3061798,
			"selected": 1
		},
		{
			"id": 302,
			"path": "\/22-boolean-logic_page_numbers.json",
			"bytes": 2145,
			"selected": 1
		},
		{
			"id": 303,
			"path": "\/22-boolean-logic_scandata.xml",
			"bytes": 4108,
			"selected": 1
		},
		{
			"id": 304,
			"path": "\/22-combinational-logic-1.mp3",
			"bytes": 9220075,
			"selected": 1
		},
		{
			"id": 305,
			"path": "\/22-combinational-logic-1.png",
			"bytes": 44996,
			"selected": 1
		},
		{
			"id": 306,
			"path": "\/22-combinational-logic-1_spectrogram.png",
			"bytes": 280242,
			"selected": 1
		},
		{
			"id": 307,
			"path": "\/22-combinational-logic-2.mp3",
			"bytes": 12326974,
			"selected": 1
		},
		{
			"id": 308,
			"path": "\/22-combinational-logic-2.png",
			"bytes": 48753,
			"selected": 1
		},
		{
			"id": 309,
			"path": "\/22-combinational-logic-2_spectrogram.png",
			"bytes": 277469,
			"selected": 1
		},
		{
			"id": 310,
			"path": "\/22-combinational-logic.pdf",
			"bytes": 1430968,
			"selected": 1
		},
		{
			"id": 311,
			"path": "\/22-combinational-logic_chocr.html.gz",
			"bytes": 80349,
			"selected": 1
		},
		{
			"id": 312,
			"path": "\/22-combinational-logic_djvu.txt",
			"bytes": 5435,
			"selected": 1
		},
		{
			"id": 313,
			"path": "\/22-combinational-logic_djvu.xml",
			"bytes": 106211,
			"selected": 1
		},
		{
			"id": 314,
			"path": "\/22-combinational-logic_hocr.html",
			"bytes": 299001,
			"selected": 1
		},
		{
			"id": 315,
			"path": "\/22-combinational-logic_hocr_pageindex.json.gz",
			"bytes": 174,
			"selected": 1
		},
		{
			"id": 316,
			"path": "\/22-combinational-logic_hocr_searchtext.txt.gz",
			"bytes": 2155,
			"selected": 1
		},
		{
			"id": 317,
			"path": "\/22-combinational-logic_jp2.zip",
			"bytes": 3859950,
			"selected": 1
		},
		{
			"id": 318,
			"path": "\/22-combinational-logic_page_numbers.json",
			"bytes": 2269,
			"selected": 1
		},
		{
			"id": 319,
			"path": "\/22-combinational-logic_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 320,
			"path": "\/23-blocks.pdf",
			"bytes": 1144358,
			"selected": 1
		},
		{
			"id": 321,
			"path": "\/23-blocks_chocr.html.gz",
			"bytes": 72751,
			"selected": 1
		},
		{
			"id": 322,
			"path": "\/23-blocks_djvu.txt",
			"bytes": 5179,
			"selected": 1
		},
		{
			"id": 323,
			"path": "\/23-blocks_djvu.xml",
			"bytes": 88926,
			"selected": 1
		},
		{
			"id": 324,
			"path": "\/23-blocks_hocr.html",
			"bytes": 196615,
			"selected": 1
		},
		{
			"id": 325,
			"path": "\/23-blocks_hocr_pageindex.json.gz",
			"bytes": 142,
			"selected": 1
		},
		{
			"id": 326,
			"path": "\/23-blocks_hocr_searchtext.txt.gz",
			"bytes": 1927,
			"selected": 1
		},
		{
			"id": 327,
			"path": "\/23-blocks_jp2.zip",
			"bytes": 1574079,
			"selected": 1
		},
		{
			"id": 328,
			"path": "\/23-blocks_page_numbers.json",
			"bytes": 2044,
			"selected": 1
		},
		{
			"id": 329,
			"path": "\/23-blocks_scandata.xml",
			"bytes": 3456,
			"selected": 1
		},
		{
			"id": 330,
			"path": "\/23-combinational-logic-blocks-1.mp3",
			"bytes": 11992085,
			"selected": 1
		},
		{
			"id": 331,
			"path": "\/23-combinational-logic-blocks-1.pdf",
			"bytes": 471837,
			"selected": 1
		},
		{
			"id": 332,
			"path": "\/23-combinational-logic-blocks-1.png",
			"bytes": 47665,
			"selected": 1
		},
		{
			"id": 333,
			"path": "\/23-combinational-logic-blocks-1_chocr.html.gz",
			"bytes": 188721,
			"selected": 1
		},
		{
			"id": 334,
			"path": "\/23-combinational-logic-blocks-1_djvu.txt",
			"bytes": 14548,
			"selected": 1
		},
		{
			"id": 335,
			"path": "\/23-combinational-logic-blocks-1_djvu.xml",
			"bytes": 214648,
			"selected": 1
		},
		{
			"id": 336,
			"path": "\/23-combinational-logic-blocks-1_hocr.html",
			"bytes": 402819,
			"selected": 1
		},
		{
			"id": 337,
			"path": "\/23-combinational-logic-blocks-1_hocr_pageindex.json.gz",
			"bytes": 126,
			"selected": 1
		},
		{
			"id": 338,
			"path": "\/23-combinational-logic-blocks-1_hocr_searchtext.txt.gz",
			"bytes": 5536,
			"selected": 1
		},
		{
			"id": 339,
			"path": "\/23-combinational-logic-blocks-1_jp2.zip",
			"bytes": 3148140,
			"selected": 1
		},
		{
			"id": 340,
			"path": "\/23-combinational-logic-blocks-1_page_numbers.json",
			"bytes": 1550,
			"selected": 1
		},
		{
			"id": 341,
			"path": "\/23-combinational-logic-blocks-1_scandata.xml",
			"bytes": 3048,
			"selected": 1
		},
		{
			"id": 342,
			"path": "\/23-combinational-logic-blocks-1_spectrogram.png",
			"bytes": 280292,
			"selected": 1
		},
		{
			"id": 343,
			"path": "\/24-blocks.pdf",
			"bytes": 1144358,
			"selected": 1
		},
		{
			"id": 344,
			"path": "\/24-blocks_chocr.html.gz",
			"bytes": 72192,
			"selected": 1
		},
		{
			"id": 345,
			"path": "\/24-blocks_djvu.txt",
			"bytes": 5178,
			"selected": 1
		},
		{
			"id": 346,
			"path": "\/24-blocks_djvu.xml",
			"bytes": 89113,
			"selected": 1
		},
		{
			"id": 347,
			"path": "\/24-blocks_hocr.html",
			"bytes": 196098,
			"selected": 1
		},
		{
			"id": 348,
			"path": "\/24-blocks_hocr_pageindex.json.gz",
			"bytes": 140,
			"selected": 1
		},
		{
			"id": 349,
			"path": "\/24-blocks_hocr_searchtext.txt.gz",
			"bytes": 1928,
			"selected": 1
		},
		{
			"id": 350,
			"path": "\/24-blocks_jp2.zip",
			"bytes": 1574079,
			"selected": 1
		},
		{
			"id": 351,
			"path": "\/24-blocks_page_numbers.json",
			"bytes": 2065,
			"selected": 1
		},
		{
			"id": 352,
			"path": "\/24-blocks_scandata.xml",
			"bytes": 3456,
			"selected": 1
		},
		{
			"id": 353,
			"path": "\/24-combinational-logic-blocks-2.mp3",
			"bytes": 10020049,
			"selected": 1
		},
		{
			"id": 354,
			"path": "\/24-combinational-logic-blocks-2.pdf",
			"bytes": 471837,
			"selected": 1
		},
		{
			"id": 355,
			"path": "\/24-combinational-logic-blocks-2.png",
			"bytes": 43870,
			"selected": 1
		},
		{
			"id": 356,
			"path": "\/24-combinational-logic-blocks-2_chocr.html.gz",
			"bytes": 188721,
			"selected": 1
		},
		{
			"id": 357,
			"path": "\/24-combinational-logic-blocks-2_djvu.txt",
			"bytes": 14548,
			"selected": 1
		},
		{
			"id": 358,
			"path": "\/24-combinational-logic-blocks-2_djvu.xml",
			"bytes": 214648,
			"selected": 1
		},
		{
			"id": 359,
			"path": "\/24-combinational-logic-blocks-2_hocr.html",
			"bytes": 402819,
			"selected": 1
		},
		{
			"id": 360,
			"path": "\/24-combinational-logic-blocks-2_hocr_pageindex.json.gz",
			"bytes": 126,
			"selected": 1
		},
		{
			"id": 361,
			"path": "\/24-combinational-logic-blocks-2_hocr_searchtext.txt.gz",
			"bytes": 5536,
			"selected": 1
		},
		{
			"id": 362,
			"path": "\/24-combinational-logic-blocks-2_jp2.zip",
			"bytes": 3148140,
			"selected": 1
		},
		{
			"id": 363,
			"path": "\/24-combinational-logic-blocks-2_page_numbers.json",
			"bytes": 1550,
			"selected": 1
		},
		{
			"id": 364,
			"path": "\/24-combinational-logic-blocks-2_scandata.xml",
			"bytes": 3048,
			"selected": 1
		},
		{
			"id": 365,
			"path": "\/24-combinational-logic-blocks-2_spectrogram.png",
			"bytes": 290366,
			"selected": 1
		},
		{
			"id": 366,
			"path": "\/25-cpu-datapath-design-1.mp3",
			"bytes": 11460128,
			"selected": 1
		},
		{
			"id": 367,
			"path": "\/25-cpu-datapath-design-1.pdf",
			"bytes": 168314,
			"selected": 1
		},
		{
			"id": 368,
			"path": "\/25-cpu-datapath-design-1.png",
			"bytes": 47711,
			"selected": 1
		},
		{
			"id": 369,
			"path": "\/25-cpu-datapath-design-1_chocr.html.gz",
			"bytes": 129266,
			"selected": 1
		},
		{
			"id": 370,
			"path": "\/25-cpu-datapath-design-1_djvu.txt",
			"bytes": 9319,
			"selected": 1
		},
		{
			"id": 371,
			"path": "\/25-cpu-datapath-design-1_djvu.xml",
			"bytes": 145123,
			"selected": 1
		},
		{
			"id": 372,
			"path": "\/25-cpu-datapath-design-1_hocr.html",
			"bytes": 303557,
			"selected": 1
		},
		{
			"id": 373,
			"path": "\/25-cpu-datapath-design-1_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 374,
			"path": "\/25-cpu-datapath-design-1_hocr_searchtext.txt.gz",
			"bytes": 3027,
			"selected": 1
		},
		{
			"id": 375,
			"path": "\/25-cpu-datapath-design-1_jp2.zip",
			"bytes": 2810720,
			"selected": 1
		},
		{
			"id": 376,
			"path": "\/25-cpu-datapath-design-1_page_numbers.json",
			"bytes": 2316,
			"selected": 1
		},
		{
			"id": 377,
			"path": "\/25-cpu-datapath-design-1_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 378,
			"path": "\/25-cpu-datapath-design-1_spectrogram.png",
			"bytes": 277065,
			"selected": 1
		},
		{
			"id": 379,
			"path": "\/26-cpu-datapath-design-2.mp3",
			"bytes": 11560124,
			"selected": 1
		},
		{
			"id": 380,
			"path": "\/26-cpu-datapath-design-2.pdf",
			"bytes": 193759,
			"selected": 1
		},
		{
			"id": 381,
			"path": "\/26-cpu-datapath-design-2.png",
			"bytes": 46888,
			"selected": 1
		},
		{
			"id": 382,
			"path": "\/26-cpu-datapath-design-2_chocr.html.gz",
			"bytes": 124443,
			"selected": 1
		},
		{
			"id": 383,
			"path": "\/26-cpu-datapath-design-2_djvu.txt",
			"bytes": 8861,
			"selected": 1
		},
		{
			"id": 384,
			"path": "\/26-cpu-datapath-design-2_djvu.xml",
			"bytes": 140801,
			"selected": 1
		},
		{
			"id": 385,
			"path": "\/26-cpu-datapath-design-2_hocr.html",
			"bytes": 297669,
			"selected": 1
		},
		{
			"id": 386,
			"path": "\/26-cpu-datapath-design-2_hocr_pageindex.json.gz",
			"bytes": 151,
			"selected": 1
		},
		{
			"id": 387,
			"path": "\/26-cpu-datapath-design-2_hocr_searchtext.txt.gz",
			"bytes": 3095,
			"selected": 1
		},
		{
			"id": 388,
			"path": "\/26-cpu-datapath-design-2_jp2.zip",
			"bytes": 2528696,
			"selected": 1
		},
		{
			"id": 389,
			"path": "\/26-cpu-datapath-design-2_page_numbers.json",
			"bytes": 2231,
			"selected": 1
		},
		{
			"id": 390,
			"path": "\/26-cpu-datapath-design-2_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 391,
			"path": "\/26-cpu-datapath-design-2_spectrogram.png",
			"bytes": 282349,
			"selected": 1
		},
		{
			"id": 392,
			"path": "\/27-cpu-control-design-1.mp3",
			"bytes": 12280059,
			"selected": 1
		},
		{
			"id": 393,
			"path": "\/27-cpu-control-design-1.pdf",
			"bytes": 182070,
			"selected": 1
		},
		{
			"id": 394,
			"path": "\/27-cpu-control-design-1.png",
			"bytes": 47001,
			"selected": 1
		},
		{
			"id": 395,
			"path": "\/27-cpu-control-design-1_chocr.html.gz",
			"bytes": 100267,
			"selected": 1
		},
		{
			"id": 396,
			"path": "\/27-cpu-control-design-1_djvu.txt",
			"bytes": 7023,
			"selected": 1
		},
		{
			"id": 397,
			"path": "\/27-cpu-control-design-1_djvu.xml",
			"bytes": 117295,
			"selected": 1
		},
		{
			"id": 398,
			"path": "\/27-cpu-control-design-1_hocr.html",
			"bytes": 261807,
			"selected": 1
		},
		{
			"id": 399,
			"path": "\/27-cpu-control-design-1_hocr_pageindex.json.gz",
			"bytes": 132,
			"selected": 1
		},
		{
			"id": 400,
			"path": "\/27-cpu-control-design-1_hocr_searchtext.txt.gz",
			"bytes": 2389,
			"selected": 1
		},
		{
			"id": 401,
			"path": "\/27-cpu-control-design-1_jp2.zip",
			"bytes": 2245215,
			"selected": 1
		},
		{
			"id": 402,
			"path": "\/27-cpu-control-design-1_page_numbers.json",
			"bytes": 1796,
			"selected": 1
		},
		{
			"id": 403,
			"path": "\/27-cpu-control-design-1_scandata.xml",
			"bytes": 3136,
			"selected": 1
		},
		{
			"id": 404,
			"path": "\/27-cpu-control-design-1_spectrogram.png",
			"bytes": 280950,
			"selected": 1
		},
		{
			"id": 405,
			"path": "\/28-cpu-control-design-2.mp3",
			"bytes": 12060108,
			"selected": 1
		},
		{
			"id": 406,
			"path": "\/28-cpu-control-design-2.pdf",
			"bytes": 266356,
			"selected": 1
		},
		{
			"id": 407,
			"path": "\/28-cpu-control-design-2.png",
			"bytes": 47679,
			"selected": 1
		},
		{
			"id": 408,
			"path": "\/28-cpu-control-design-2_chocr.html.gz",
			"bytes": 143421,
			"selected": 1
		},
		{
			"id": 409,
			"path": "\/28-cpu-control-design-2_djvu.txt",
			"bytes": 9827,
			"selected": 1
		},
		{
			"id": 410,
			"path": "\/28-cpu-control-design-2_djvu.xml",
			"bytes": 173333,
			"selected": 1
		},
		{
			"id": 411,
			"path": "\/28-cpu-control-design-2_hocr.html",
			"bytes": 408764,
			"selected": 1
		},
		{
			"id": 412,
			"path": "\/28-cpu-control-design-2_hocr_pageindex.json.gz",
			"bytes": 167,
			"selected": 1
		},
		{
			"id": 413,
			"path": "\/28-cpu-control-design-2_hocr_searchtext.txt.gz",
			"bytes": 3100,
			"selected": 1
		},
		{
			"id": 414,
			"path": "\/28-cpu-control-design-2_jp2.zip",
			"bytes": 3374224,
			"selected": 1
		},
		{
			"id": 415,
			"path": "\/28-cpu-control-design-2_page_numbers.json",
			"bytes": 2663,
			"selected": 1
		},
		{
			"id": 416,
			"path": "\/28-cpu-control-design-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 417,
			"path": "\/28-cpu-control-design-2_spectrogram.png",
			"bytes": 277904,
			"selected": 1
		},
		{
			"id": 418,
			"path": "\/29-pipelining-1.mp3",
			"bytes": 11268075,
			"selected": 1
		},
		{
			"id": 419,
			"path": "\/29-pipelining-1.pdf",
			"bytes": 278732,
			"selected": 1
		},
		{
			"id": 420,
			"path": "\/29-pipelining-1.png",
			"bytes": 46949,
			"selected": 1
		},
		{
			"id": 421,
			"path": "\/29-pipelining-1_chocr.html.gz",
			"bytes": 144637,
			"selected": 1
		},
		{
			"id": 422,
			"path": "\/29-pipelining-1_djvu.txt",
			"bytes": 10346,
			"selected": 1
		},
		{
			"id": 423,
			"path": "\/29-pipelining-1_djvu.xml",
			"bytes": 164407,
			"selected": 1
		},
		{
			"id": 424,
			"path": "\/29-pipelining-1_hocr.html",
			"bytes": 360430,
			"selected": 1
		},
		{
			"id": 425,
			"path": "\/29-pipelining-1_hocr_pageindex.json.gz",
			"bytes": 170,
			"selected": 1
		},
		{
			"id": 426,
			"path": "\/29-pipelining-1_hocr_searchtext.txt.gz",
			"bytes": 3950,
			"selected": 1
		},
		{
			"id": 427,
			"path": "\/29-pipelining-1_jp2.zip",
			"bytes": 3357295,
			"selected": 1
		},
		{
			"id": 428,
			"path": "\/29-pipelining-1_page_numbers.json",
			"bytes": 2673,
			"selected": 1
		},
		{
			"id": 429,
			"path": "\/29-pipelining-1_scandata.xml",
			"bytes": 4548,
			"selected": 1
		},
		{
			"id": 430,
			"path": "\/29-pipelining-1_spectrogram.png",
			"bytes": 280249,
			"selected": 1
		},
		{
			"id": 431,
			"path": "\/30-pipelining-2.mp3",
			"bytes": 12368039,
			"selected": 1
		},
		{
			"id": 432,
			"path": "\/30-pipelining-2.pdf",
			"bytes": 309503,
			"selected": 1
		},
		{
			"id": 433,
			"path": "\/30-pipelining-2.png",
			"bytes": 61399,
			"selected": 1
		},
		{
			"id": 434,
			"path": "\/30-pipelining-2_chocr.html.gz",
			"bytes": 139743,
			"selected": 1
		},
		{
			"id": 435,
			"path": "\/30-pipelining-2_djvu.txt",
			"bytes": 9902,
			"selected": 1
		},
		{
			"id": 436,
			"path": "\/30-pipelining-2_djvu.xml",
			"bytes": 158281,
			"selected": 1
		},
		{
			"id": 437,
			"path": "\/30-pipelining-2_hocr.html",
			"bytes": 351352,
			"selected": 1
		},
		{
			"id": 438,
			"path": "\/30-pipelining-2_hocr_pageindex.json.gz",
			"bytes": 180,
			"selected": 1
		},
		{
			"id": 439,
			"path": "\/30-pipelining-2_hocr_searchtext.txt.gz",
			"bytes": 3367,
			"selected": 1
		},
		{
			"id": 440,
			"path": "\/30-pipelining-2_jp2.zip",
			"bytes": 3549921,
			"selected": 1
		},
		{
			"id": 441,
			"path": "\/30-pipelining-2_page_numbers.json",
			"bytes": 2819,
			"selected": 1
		},
		{
			"id": 442,
			"path": "\/30-pipelining-2_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 443,
			"path": "\/30-pipelining-2_spectrogram.png",
			"bytes": 273258,
			"selected": 1
		},
		{
			"id": 444,
			"path": "\/31-caches-1.mp3",
			"bytes": 11820095,
			"selected": 1
		},
		{
			"id": 445,
			"path": "\/31-caches-1.pdf",
			"bytes": 249321,
			"selected": 1
		},
		{
			"id": 446,
			"path": "\/31-caches-1.png",
			"bytes": 62019,
			"selected": 1
		},
		{
			"id": 447,
			"path": "\/31-caches-1_chocr.html.gz",
			"bytes": 200698,
			"selected": 1
		},
		{
			"id": 448,
			"path": "\/31-caches-1_djvu.txt",
			"bytes": 14470,
			"selected": 1
		},
		{
			"id": 449,
			"path": "\/31-caches-1_djvu.xml",
			"bytes": 228537,
			"selected": 1
		},
		{
			"id": 450,
			"path": "\/31-caches-1_hocr.html",
			"bytes": 505693,
			"selected": 1
		},
		{
			"id": 451,
			"path": "\/31-caches-1_hocr_pageindex.json.gz",
			"bytes": 273,
			"selected": 1
		},
		{
			"id": 452,
			"path": "\/31-caches-1_hocr_searchtext.txt.gz",
			"bytes": 4788,
			"selected": 1
		},
		{
			"id": 453,
			"path": "\/31-caches-1_jp2.zip",
			"bytes": 5326451,
			"selected": 1
		},
		{
			"id": 454,
			"path": "\/31-caches-1_page_numbers.json",
			"bytes": 4851,
			"selected": 1
		},
		{
			"id": 455,
			"path": "\/31-caches-1_scandata.xml",
			"bytes": 7616,
			"selected": 1
		},
		{
			"id": 456,
			"path": "\/31-caches-1_spectrogram.png",
			"bytes": 273867,
			"selected": 1
		},
		{
			"id": 457,
			"path": "\/32-caches-2.mp3",
			"bytes": 11924062,
			"selected": 1
		},
		{
			"id": 458,
			"path": "\/32-caches-2.pdf",
			"bytes": 197261,
			"selected": 1
		},
		{
			"id": 459,
			"path": "\/32-caches-2.png",
			"bytes": 64334,
			"selected": 1
		},
		{
			"id": 460,
			"path": "\/32-caches-2_chocr.html.gz",
			"bytes": 154848,
			"selected": 1
		},
		{
			"id": 461,
			"path": "\/32-caches-2_djvu.txt",
			"bytes": 11088,
			"selected": 1
		},
		{
			"id": 462,
			"path": "\/32-caches-2_djvu.xml",
			"bytes": 183801,
			"selected": 1
		},
		{
			"id": 463,
			"path": "\/32-caches-2_hocr.html",
			"bytes": 411904,
			"selected": 1
		},
		{
			"id": 464,
			"path": "\/32-caches-2_hocr_pageindex.json.gz",
			"bytes": 169,
			"selected": 1
		},
		{
			"id": 465,
			"path": "\/32-caches-2_hocr_searchtext.txt.gz",
			"bytes": 4077,
			"selected": 1
		},
		{
			"id": 466,
			"path": "\/32-caches-2_jp2.zip",
			"bytes": 3063200,
			"selected": 1
		},
		{
			"id": 467,
			"path": "\/32-caches-2_page_numbers.json",
			"bytes": 2535,
			"selected": 1
		},
		{
			"id": 468,
			"path": "\/32-caches-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 469,
			"path": "\/32-caches-2_spectrogram.png",
			"bytes": 271774,
			"selected": 1
		},
		{
			"id": 470,
			"path": "\/33-caches-3.mp3",
			"bytes": 12312137,
			"selected": 1
		},
		{
			"id": 471,
			"path": "\/33-caches-3.pdf",
			"bytes": 2152544,
			"selected": 1
		},
		{
			"id": 472,
			"path": "\/33-caches-3.png",
			"bytes": 43673,
			"selected": 1
		},
		{
			"id": 473,
			"path": "\/33-caches-3_chocr.html.gz",
			"bytes": 146720,
			"selected": 1
		},
		{
			"id": 474,
			"path": "\/33-caches-3_djvu.txt",
			"bytes": 10421,
			"selected": 1
		},
		{
			"id": 475,
			"path": "\/33-caches-3_djvu.xml",
			"bytes": 170977,
			"selected": 1
		},
		{
			"id": 476,
			"path": "\/33-caches-3_hocr.html",
			"bytes": 374426,
			"selected": 1
		},
		{
			"id": 477,
			"path": "\/33-caches-3_hocr_pageindex.json.gz",
			"bytes": 178,
			"selected": 1
		},
		{
			"id": 478,
			"path": "\/33-caches-3_hocr_searchtext.txt.gz",
			"bytes": 4009,
			"selected": 1
		},
		{
			"id": 479,
			"path": "\/33-caches-3_jp2.zip",
			"bytes": 3187263,
			"selected": 1
		},
		{
			"id": 480,
			"path": "\/33-caches-3_page_numbers.json",
			"bytes": 2760,
			"selected": 1
		},
		{
			"id": 481,
			"path": "\/33-caches-3_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 482,
			"path": "\/33-caches-3_spectrogram.png",
			"bytes": 280474,
			"selected": 1
		},
		{
			"id": 483,
			"path": "\/34-virtual-memory-1.mp3",
			"bytes": 12080169,
			"selected": 1
		},
		{
			"id": 484,
			"path": "\/34-virtual-memory-1.pdf",
			"bytes": 181002,
			"selected": 1
		},
		{
			"id": 485,
			"path": "\/34-virtual-memory-1.png",
			"bytes": 46222,
			"selected": 1
		},
		{
			"id": 486,
			"path": "\/34-virtual-memory-1_chocr.html.gz",
			"bytes": 133454,
			"selected": 1
		},
		{
			"id": 487,
			"path": "\/34-virtual-memory-1_djvu.txt",
			"bytes": 9517,
			"selected": 1
		},
		{
			"id": 488,
			"path": "\/34-virtual-memory-1_djvu.xml",
			"bytes": 144906,
			"selected": 1
		},
		{
			"id": 489,
			"path": "\/34-virtual-memory-1_hocr.html",
			"bytes": 312738,
			"selected": 1
		},
		{
			"id": 490,
			"path": "\/34-virtual-memory-1_hocr_pageindex.json.gz",
			"bytes": 160,
			"selected": 1
		},
		{
			"id": 491,
			"path": "\/34-virtual-memory-1_hocr_searchtext.txt.gz",
			"bytes": 3530,
			"selected": 1
		},
		{
			"id": 492,
			"path": "\/34-virtual-memory-1_jp2.zip",
			"bytes": 3141795,
			"selected": 1
		},
		{
			"id": 493,
			"path": "\/34-virtual-memory-1_page_numbers.json",
			"bytes": 2398,
			"selected": 1
		},
		{
			"id": 494,
			"path": "\/34-virtual-memory-1_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 495,
			"path": "\/34-virtual-memory-1_spectrogram.png",
			"bytes": 279355,
			"selected": 1
		},
		{
			"id": 496,
			"path": "\/35-virtual-memory-2.mp3",
			"bytes": 11900134,
			"selected": 1
		},
		{
			"id": 497,
			"path": "\/35-virtual-memory-2.pdf",
			"bytes": 230145,
			"selected": 1
		},
		{
			"id": 498,
			"path": "\/35-virtual-memory-2.png",
			"bytes": 46198,
			"selected": 1
		},
		{
			"id": 499,
			"path": "\/35-virtual-memory-2_chocr.html.gz",
			"bytes": 156498,
			"selected": 1
		},
		{
			"id": 500,
			"path": "\/35-virtual-memory-2_djvu.txt",
			"bytes": 11204,
			"selected": 1
		},
		{
			"id": 501,
			"path": "\/35-virtual-memory-2_djvu.xml",
			"bytes": 182223,
			"selected": 1
		},
		{
			"id": 502,
			"path": "\/35-virtual-memory-2_hocr.html",
			"bytes": 418467,
			"selected": 1
		},
		{
			"id": 503,
			"path": "\/35-virtual-memory-2_hocr_pageindex.json.gz",
			"bytes": 169,
			"selected": 1
		},
		{
			"id": 504,
			"path": "\/35-virtual-memory-2_hocr_searchtext.txt.gz",
			"bytes": 3935,
			"selected": 1
		},
		{
			"id": 505,
			"path": "\/35-virtual-memory-2_jp2.zip",
			"bytes": 3529460,
			"selected": 1
		},
		{
			"id": 506,
			"path": "\/35-virtual-memory-2_page_numbers.json",
			"bytes": 2640,
			"selected": 1
		},
		{
			"id": 507,
			"path": "\/35-virtual-memory-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 508,
			"path": "\/35-virtual-memory-2_spectrogram.png",
			"bytes": 281408,
			"selected": 1
		},
		{
			"id": 509,
			"path": "\/36-io.mp3",
			"bytes": 12020088,
			"selected": 1
		},
		{
			"id": 510,
			"path": "\/36-io.pdf",
			"bytes": 299907,
			"selected": 1
		},
		{
			"id": 511,
			"path": "\/36-io.png",
			"bytes": 45659,
			"selected": 1
		},
		{
			"id": 512,
			"path": "\/36-io_chocr.html.gz",
			"bytes": 208058,
			"selected": 1
		},
		{
			"id": 513,
			"path": "\/36-io_djvu.txt",
			"bytes": 14952,
			"selected": 1
		},
		{
			"id": 514,
			"path": "\/36-io_djvu.xml",
			"bytes": 225463,
			"selected": 1
		},
		{
			"id": 515,
			"path": "\/36-io_hocr.html",
			"bytes": 496618,
			"selected": 1
		},
		{
			"id": 516,
			"path": "\/36-io_hocr_pageindex.json.gz",
			"bytes": 204,
			"selected": 1
		},
		{
			"id": 517,
			"path": "\/36-io_hocr_searchtext.txt.gz",
			"bytes": 5459,
			"selected": 1
		},
		{
			"id": 518,
			"path": "\/36-io_jp2.zip",
			"bytes": 4369980,
			"selected": 1
		},
		{
			"id": 519,
			"path": "\/36-io_page_numbers.json",
			"bytes": 3317,
			"selected": 1
		},
		{
			"id": 520,
			"path": "\/36-io_scandata.xml",
			"bytes": 5376,
			"selected": 1
		},
		{
			"id": 521,
			"path": "\/36-io_spectrogram.png",
			"bytes": 283738,
			"selected": 1
		},
		{
			"id": 522,
			"path": "\/37-networks.mp3",
			"bytes": 12332095,
			"selected": 1
		},
		{
			"id": 523,
			"path": "\/37-networks.pdf",
			"bytes": 776128,
			"selected": 1
		},
		{
			"id": 524,
			"path": "\/37-networks.png",
			"bytes": 46149,
			"selected": 1
		},
		{
			"id": 525,
			"path": "\/37-networks_chocr.html.gz",
			"bytes": 227408,
			"selected": 1
		},
		{
			"id": 526,
			"path": "\/37-networks_djvu.txt",
			"bytes": 16965,
			"selected": 1
		},
		{
			"id": 527,
			"path": "\/37-networks_djvu.xml",
			"bytes": 241728,
			"selected": 1
		},
		{
			"id": 528,
			"path": "\/37-networks_hocr.html",
			"bytes": 509775,
			"selected": 1
		},
		{
			"id": 529,
			"path": "\/37-networks_hocr_pageindex.json.gz",
			"bytes": 165,
			"selected": 1
		},
		{
			"id": 530,
			"path": "\/37-networks_hocr_searchtext.txt.gz",
			"bytes": 6881,
			"selected": 1
		},
		{
			"id": 531,
			"path": "\/37-networks_jp2.zip",
			"bytes": 3111734,
			"selected": 1
		},
		{
			"id": 532,
			"path": "\/37-networks_page_numbers.json",
			"bytes": 2449,
			"selected": 1
		},
		{
			"id": 533,
			"path": "\/37-networks_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 534,
			"path": "\/37-networks_spectrogram.png",
			"bytes": 278315,
			"selected": 1
		},
		{
			"id": 535,
			"path": "\/38-disks.mp3",
			"bytes": 11984039,
			"selected": 1
		},
		{
			"id": 536,
			"path": "\/38-disks.pdf",
			"bytes": 6304288,
			"selected": 1
		},
		{
			"id": 537,
			"path": "\/38-disks.png",
			"bytes": 46571,
			"selected": 1
		},
		{
			"id": 538,
			"path": "\/38-disks_chocr.html.gz",
			"bytes": 185381,
			"selected": 1
		},
		{
			"id": 539,
			"path": "\/38-disks_djvu.txt",
			"bytes": 13177,
			"selected": 1
		},
		{
			"id": 540,
			"path": "\/38-disks_djvu.xml",
			"bytes": 208399,
			"selected": 1
		},
		{
			"id": 541,
			"path": "\/38-disks_hocr.html",
			"bytes": 453998,
			"selected": 1
		},
		{
			"id": 542,
			"path": "\/38-disks_hocr_pageindex.json.gz",
			"bytes": 191,
			"selected": 1
		},
		{
			"id": 543,
			"path": "\/38-disks_hocr_searchtext.txt.gz",
			"bytes": 5188,
			"selected": 1
		},
		{
			"id": 544,
			"path": "\/38-disks_jp2.zip",
			"bytes": 3995258,
			"selected": 1
		},
		{
			"id": 545,
			"path": "\/38-disks_page_numbers.json",
			"bytes": 3113,
			"selected": 1
		},
		{
			"id": 546,
			"path": "\/38-disks_scandata.xml",
			"bytes": 5056,
			"selected": 1
		},
		{
			"id": 547,
			"path": "\/38-disks_spectrogram.png",
			"bytes": 275955,
			"selected": 1
		},
		{
			"id": 548,
			"path": "\/39-performance-1.mp3",
			"bytes": 12140042,
			"selected": 1
		},
		{
			"id": 549,
			"path": "\/39-performance-1.png",
			"bytes": 47261,
			"selected": 1
		},
		{
			"id": 550,
			"path": "\/39-performance-1_spectrogram.png",
			"bytes": 281600,
			"selected": 1
		},
		{
			"id": 551,
			"path": "\/39-performance-2.mp3",
			"bytes": 11912046,
			"selected": 1
		},
		{
			"id": 552,
			"path": "\/39-performance-2.png",
			"bytes": 47209,
			"selected": 1
		},
		{
			"id": 553,
			"path": "\/39-performance-2_spectrogram.png",
			"bytes": 278590,
			"selected": 1
		},
		{
			"id": 554,
			"path": "\/39-performance.pdf",
			"bytes": 316804,
			"selected": 1
		},
		{
			"id": 555,
			"path": "\/39-performance_chocr.html.gz",
			"bytes": 186813,
			"selected": 1
		},
		{
			"id": 556,
			"path": "\/39-performance_djvu.txt",
			"bytes": 13635,
			"selected": 1
		},
		{
			"id": 557,
			"path": "\/39-performance_djvu.xml",
			"bytes": 191756,
			"selected": 1
		},
		{
			"id": 558,
			"path": "\/39-performance_hocr.html",
			"bytes": 398028,
			"selected": 1
		},
		{
			"id": 559,
			"path": "\/39-performance_hocr_pageindex.json.gz",
			"bytes": 183,
			"selected": 1
		},
		{
			"id": 560,
			"path": "\/39-performance_hocr_searchtext.txt.gz",
			"bytes": 5112,
			"selected": 1
		},
		{
			"id": 561,
			"path": "\/39-performance_jp2.zip",
			"bytes": 3869180,
			"selected": 1
		},
		{
			"id": 562,
			"path": "\/39-performance_page_numbers.json",
			"bytes": 2872,
			"selected": 1
		},
		{
			"id": 563,
			"path": "\/39-performance_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 564,
			"path": "\/40-x86.mp3",
			"bytes": 8760111,
			"selected": 1
		},
		{
			"id": 565,
			"path": "\/40-x86.pdf",
			"bytes": 248008,
			"selected": 1
		},
		{
			"id": 566,
			"path": "\/40-x86.png",
			"bytes": 47230,
			"selected": 1
		},
		{
			"id": 567,
			"path": "\/40-x86_chocr.html.gz",
			"bytes": 249946,
			"selected": 1
		},
		{
			"id": 568,
			"path": "\/40-x86_djvu.txt",
			"bytes": 17991,
			"selected": 1
		},
		{
			"id": 569,
			"path": "\/40-x86_djvu.xml",
			"bytes": 265571,
			"selected": 1
		},
		{
			"id": 570,
			"path": "\/40-x86_hocr.html",
			"bytes": 572490,
			"selected": 1
		},
		{
			"id": 571,
			"path": "\/40-x86_hocr_pageindex.json.gz",
			"bytes": 224,
			"selected": 1
		},
		{
			"id": 572,
			"path": "\/40-x86_hocr_searchtext.txt.gz",
			"bytes": 6862,
			"selected": 1
		},
		{
			"id": 573,
			"path": "\/40-x86_jp2.zip",
			"bytes": 4479043,
			"selected": 1
		},
		{
			"id": 574,
			"path": "\/40-x86_page_numbers.json",
			"bytes": 3602,
			"selected": 1
		},
		{
			"id": 575,
			"path": "\/40-x86_scandata.xml",
			"bytes": 6016,
			"selected": 1
		},
		{
			"id": 576,
			"path": "\/40-x86_spectrogram.png",
			"bytes": 279919,
			"selected": 1
		},
		{
			"id": 577,
			"path": "\/41-introduction-to-reconfigurable-computing.mp3",
			"bytes": 12072124,
			"selected": 1
		},
		{
			"id": 578,
			"path": "\/41-introduction-to-reconfigurable-computing.png",
			"bytes": 48058,
			"selected": 1
		},
		{
			"id": 579,
			"path": "\/41-introduction-to-reconfigurable-computing.ppt",
			"bytes": 4370432,
			"selected": 1
		},
		{
			"id": 580,
			"path": "\/41-introduction-to-reconfigurable-computing_spectrogram.png",
			"bytes": 269602,
			"selected": 1
		},
		{
			"id": 581,
			"path": "\/42-class-summary.mp3",
			"bytes": 10680111,
			"selected": 1
		},
		{
			"id": 582,
			"path": "\/42-class-summary.png",
			"bytes": 46060,
			"selected": 1
		},
		{
			"id": 583,
			"path": "\/42-class-summary_spectrogram.png",
			"bytes": 282868,
			"selected": 1
		},
		{
			"id": 584,
			"path": "\/README.txt",
			"bytes": 62,
			"selected": 1
		},
		{
			"id": 585,
			"path": "\/__ia_thumb.jpg",
			"bytes": 3823,
			"selected": 1
		},
		{
			"id": 586,
			"path": "\/uc-berkeley-cs61c-great-ideas-in-computer-architecture_meta.sqlite",
			"bytes": 278528,
			"selected": 1
		},
		{
			"id": 587,
			"path": "\/uc-berkeley-cs61c-great-ideas-in-computer-architecture_meta.xml",
			"bytes": 1342,
			"selected": 1
		},
		{
			"id": 588,
			"path": "\/mars\/mars.jar",
			"bytes": 4169142,
			"selected": 1
		},
		{
			"id": 589,
			"path": "\/mars\/mips.asm",
			"bytes": 145,
			"selected": 1
		}
	],
	"links": [
	],
	"speed": 1141000,
	"seeders": 4
}
        """
                .trimIndent()
        val item: TorrentItem = jsonAdapter.fromJson(json)!!

        val torrentStructure: Node<TorrentFileItem> = getFilesNodes(item, false)

        print(torrentStructure)
        assert(torrentStructure.children.isNotEmpty())
    }
    @Test
    fun testSingleTorrentFile() {
        val json =
            """
        {"id": "C46XUVV45KHTA",
            "filename": "PlaneShift-v0.6.3-x64.exe",
            "original_filename": "PlaneShift-v0.6.3-x64.exe",
            "hash": "8307bf1f543fa33e7b9e49f8492443a3b20e0c7d",
            "bytes": 1007432683,
            "original_bytes": 1007432683,
            "host": "real-debrid.com",
            "split": 2000,
            "progress": 100,
            "status": "downloaded",
            "added": "2022-10-31T10:39:02.000Z",
            "files": [
            {
                "id": 1,
                "path": "\/PlaneShift-v0.6.3-x64.exe",
                "bytes": 1007432683,
                "selected": 1
            }
            ],
            "links": [
            "https:\/\/real-debrid.com\/d\/AKYOSR2XYW5NM"
            ],
            "ended": "2022-10-31T10:43:07.000Z"
        }
        """
                .trimIndent()
        val item: TorrentItem = jsonAdapter.fromJson(json)!!

        val torrentStructure: Node<TorrentFileItem> = getFilesNodes(item, false)

        println(torrentStructure)
        assert(torrentStructure.children.isNotEmpty())
    }

    @Test
    fun torrentItemPropertyChange() {
        val json =
            """
    {
	"id": "XGYA5QSAA7JI4",
	"filename": "uc-berkeley-cs61c-great-ideas-in-computer-architecture",
	"original_filename": "uc-berkeley-cs61c-great-ideas-in-computer-architecture",
	"hash": "7f53b1ae54fe80b6c98b4e263e59f5b08061000c",
	"bytes": 748852727,
	"original_bytes": 748852727,
	"host": "real-debrid.com",
	"split": 2000,
	"progress": 0,
	"status": "downloading",
	"added": "2022-10-31T10:40:30.000Z",
	"files": [
		{
			"id": 1,
			"path": "\/01-course-introduction.mp3",
			"bytes": 11440065,
			"selected": 1
		},
		{
			"id": 2,
			"path": "\/01-course-introduction.pdf",
			"bytes": 533998,
			"selected": 1
		},
		{
			"id": 3,
			"path": "\/01-course-introduction.png",
			"bytes": 60749,
			"selected": 1
		},
		{
			"id": 4,
			"path": "\/01-course-introduction_chocr.html.gz",
			"bytes": 157001,
			"selected": 1
		},
		{
			"id": 5,
			"path": "\/01-course-introduction_djvu.txt",
			"bytes": 11172,
			"selected": 1
		},
		{
			"id": 6,
			"path": "\/01-course-introduction_djvu.xml",
			"bytes": 180119,
			"selected": 1
		},
		{
			"id": 7,
			"path": "\/01-course-introduction_hocr.html",
			"bytes": 430184,
			"selected": 1
		},
		{
			"id": 8,
			"path": "\/01-course-introduction_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 9,
			"path": "\/01-course-introduction_hocr_searchtext.txt.gz",
			"bytes": 4498,
			"selected": 1
		},
		{
			"id": 10,
			"path": "\/01-course-introduction_jp2.zip",
			"bytes": 3125033,
			"selected": 1
		},
		{
			"id": 11,
			"path": "\/01-course-introduction_page_numbers.json",
			"bytes": 2491,
			"selected": 1
		},
		{
			"id": 12,
			"path": "\/01-course-introduction_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 13,
			"path": "\/01-course-introduction_spectrogram.png",
			"bytes": 265192,
			"selected": 1
		},
		{
			"id": 14,
			"path": "\/02-number-representation.mp3",
			"bytes": 11880176,
			"selected": 1
		},
		{
			"id": 15,
			"path": "\/02-number-representation.pdf",
			"bytes": 144891,
			"selected": 1
		},
		{
			"id": 16,
			"path": "\/02-number-representation.png",
			"bytes": 62304,
			"selected": 1
		},
		{
			"id": 17,
			"path": "\/02-number-representation_chocr.html.gz",
			"bytes": 149880,
			"selected": 1
		},
		{
			"id": 18,
			"path": "\/02-number-representation_djvu.txt",
			"bytes": 10707,
			"selected": 1
		},
		{
			"id": 19,
			"path": "\/02-number-representation_djvu.xml",
			"bytes": 174661,
			"selected": 1
		},
		{
			"id": 20,
			"path": "\/02-number-representation_hocr.html",
			"bytes": 396133,
			"selected": 1
		},
		{
			"id": 21,
			"path": "\/02-number-representation_hocr_pageindex.json.gz",
			"bytes": 168,
			"selected": 1
		},
		{
			"id": 22,
			"path": "\/02-number-representation_hocr_searchtext.txt.gz",
			"bytes": 3916,
			"selected": 1
		},
		{
			"id": 23,
			"path": "\/02-number-representation_jp2.zip",
			"bytes": 3107169,
			"selected": 1
		},
		{
			"id": 24,
			"path": "\/02-number-representation_page_numbers.json",
			"bytes": 2720,
			"selected": 1
		},
		{
			"id": 25,
			"path": "\/02-number-representation_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 26,
			"path": "\/02-number-representation_spectrogram.png",
			"bytes": 271329,
			"selected": 1
		},
		{
			"id": 27,
			"path": "\/03-introduction-to-c.mp3",
			"bytes": 12180062,
			"selected": 1
		},
		{
			"id": 28,
			"path": "\/03-introduction-to-c.pdf",
			"bytes": 346414,
			"selected": 1
		},
		{
			"id": 29,
			"path": "\/03-introduction-to-c.png",
			"bytes": 46951,
			"selected": 1
		},
		{
			"id": 30,
			"path": "\/03-introduction-to-c_chocr.html.gz",
			"bytes": 116092,
			"selected": 1
		},
		{
			"id": 31,
			"path": "\/03-introduction-to-c_djvu.txt",
			"bytes": 8313,
			"selected": 1
		},
		{
			"id": 32,
			"path": "\/03-introduction-to-c_djvu.xml",
			"bytes": 131261,
			"selected": 1
		},
		{
			"id": 33,
			"path": "\/03-introduction-to-c_hocr.html",
			"bytes": 279842,
			"selected": 1
		},
		{
			"id": 34,
			"path": "\/03-introduction-to-c_hocr_pageindex.json.gz",
			"bytes": 149,
			"selected": 1
		},
		{
			"id": 35,
			"path": "\/03-introduction-to-c_hocr_searchtext.txt.gz",
			"bytes": 3265,
			"selected": 1
		},
		{
			"id": 36,
			"path": "\/03-introduction-to-c_jp2.zip",
			"bytes": 2308115,
			"selected": 1
		},
		{
			"id": 37,
			"path": "\/03-introduction-to-c_page_numbers.json",
			"bytes": 2356,
			"selected": 1
		},
		{
			"id": 38,
			"path": "\/03-introduction-to-c_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 39,
			"path": "\/03-introduction-to-c_spectrogram.png",
			"bytes": 281667,
			"selected": 1
		},
		{
			"id": 40,
			"path": "\/03-notes-on-c-harvey.pdf",
			"bytes": 156441,
			"selected": 1
		},
		{
			"id": 41,
			"path": "\/03-notes-on-c-harvey_chocr.html.gz",
			"bytes": 672455,
			"selected": 1
		},
		{
			"id": 42,
			"path": "\/03-notes-on-c-harvey_djvu.txt",
			"bytes": 53263,
			"selected": 1
		},
		{
			"id": 43,
			"path": "\/03-notes-on-c-harvey_djvu.xml",
			"bytes": 697183,
			"selected": 1
		},
		{
			"id": 44,
			"path": "\/03-notes-on-c-harvey_hocr.html",
			"bytes": 1274846,
			"selected": 1
		},
		{
			"id": 45,
			"path": "\/03-notes-on-c-harvey_hocr_pageindex.json.gz",
			"bytes": 247,
			"selected": 1
		},
		{
			"id": 46,
			"path": "\/03-notes-on-c-harvey_hocr_searchtext.txt.gz",
			"bytes": 19346,
			"selected": 1
		},
		{
			"id": 47,
			"path": "\/03-notes-on-c-harvey_jp2.zip",
			"bytes": 10678603,
			"selected": 1
		},
		{
			"id": 48,
			"path": "\/03-notes-on-c-harvey_page_numbers.json",
			"bytes": 3815,
			"selected": 1
		},
		{
			"id": 49,
			"path": "\/03-notes-on-c-harvey_scandata.xml",
			"bytes": 6940,
			"selected": 1
		},
		{
			"id": 50,
			"path": "\/04-c-pointers-and-arrays.mp3",
			"bytes": 10820127,
			"selected": 1
		},
		{
			"id": 51,
			"path": "\/04-c-pointers-and-arrays.pdf",
			"bytes": 140495,
			"selected": 1
		},
		{
			"id": 52,
			"path": "\/04-c-pointers-and-arrays.png",
			"bytes": 45776,
			"selected": 1
		},
		{
			"id": 53,
			"path": "\/04-c-pointers-and-arrays_chocr.html.gz",
			"bytes": 128855,
			"selected": 1
		},
		{
			"id": 54,
			"path": "\/04-c-pointers-and-arrays_djvu.txt",
			"bytes": 9240,
			"selected": 1
		},
		{
			"id": 55,
			"path": "\/04-c-pointers-and-arrays_djvu.xml",
			"bytes": 142829,
			"selected": 1
		},
		{
			"id": 56,
			"path": "\/04-c-pointers-and-arrays_hocr.html",
			"bytes": 297646,
			"selected": 1
		},
		{
			"id": 57,
			"path": "\/04-c-pointers-and-arrays_hocr_pageindex.json.gz",
			"bytes": 150,
			"selected": 1
		},
		{
			"id": 58,
			"path": "\/04-c-pointers-and-arrays_hocr_searchtext.txt.gz",
			"bytes": 3541,
			"selected": 1
		},
		{
			"id": 59,
			"path": "\/04-c-pointers-and-arrays_jp2.zip",
			"bytes": 2555701,
			"selected": 1
		},
		{
			"id": 60,
			"path": "\/04-c-pointers-and-arrays_page_numbers.json",
			"bytes": 2165,
			"selected": 1
		},
		{
			"id": 61,
			"path": "\/04-c-pointers-and-arrays_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 62,
			"path": "\/04-c-pointers-and-arrays_spectrogram.png",
			"bytes": 292152,
			"selected": 1
		},
		{
			"id": 63,
			"path": "\/05-c-structs-and-memory-management.mp3",
			"bytes": 11400046,
			"selected": 1
		},
		{
			"id": 64,
			"path": "\/05-c-structs-and-memory-management.pdf",
			"bytes": 146981,
			"selected": 1
		},
		{
			"id": 65,
			"path": "\/05-c-structs-and-memory-management.png",
			"bytes": 45782,
			"selected": 1
		},
		{
			"id": 66,
			"path": "\/05-c-structs-and-memory-management_chocr.html.gz",
			"bytes": 132613,
			"selected": 1
		},
		{
			"id": 67,
			"path": "\/05-c-structs-and-memory-management_djvu.txt",
			"bytes": 9414,
			"selected": 1
		},
		{
			"id": 68,
			"path": "\/05-c-structs-and-memory-management_djvu.xml",
			"bytes": 158585,
			"selected": 1
		},
		{
			"id": 69,
			"path": "\/05-c-structs-and-memory-management_hocr.html",
			"bytes": 356434,
			"selected": 1
		},
		{
			"id": 70,
			"path": "\/05-c-structs-and-memory-management_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 71,
			"path": "\/05-c-structs-and-memory-management_hocr_searchtext.txt.gz",
			"bytes": 2916,
			"selected": 1
		},
		{
			"id": 72,
			"path": "\/05-c-structs-and-memory-management_jp2.zip",
			"bytes": 2405130,
			"selected": 1
		},
		{
			"id": 73,
			"path": "\/05-c-structs-and-memory-management_page_numbers.json",
			"bytes": 2439,
			"selected": 1
		},
		{
			"id": 74,
			"path": "\/05-c-structs-and-memory-management_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 75,
			"path": "\/05-c-structs-and-memory-management_spectrogram.png",
			"bytes": 285901,
			"selected": 1
		},
		{
			"id": 76,
			"path": "\/06-c-memory-management-1.pdf",
			"bytes": 176254,
			"selected": 1
		},
		{
			"id": 77,
			"path": "\/06-c-memory-management-1_chocr.html.gz",
			"bytes": 173944,
			"selected": 1
		},
		{
			"id": 78,
			"path": "\/06-c-memory-management-1_djvu.txt",
			"bytes": 12576,
			"selected": 1
		},
		{
			"id": 79,
			"path": "\/06-c-memory-management-1_djvu.xml",
			"bytes": 193722,
			"selected": 1
		},
		{
			"id": 80,
			"path": "\/06-c-memory-management-1_hocr.html",
			"bytes": 414609,
			"selected": 1
		},
		{
			"id": 81,
			"path": "\/06-c-memory-management-1_hocr_pageindex.json.gz",
			"bytes": 193,
			"selected": 1
		},
		{
			"id": 82,
			"path": "\/06-c-memory-management-1_hocr_searchtext.txt.gz",
			"bytes": 4181,
			"selected": 1
		},
		{
			"id": 83,
			"path": "\/06-c-memory-management-1_jp2.zip",
			"bytes": 3682929,
			"selected": 1
		},
		{
			"id": 84,
			"path": "\/06-c-memory-management-1_page_numbers.json",
			"bytes": 3100,
			"selected": 1
		},
		{
			"id": 85,
			"path": "\/06-c-memory-management-1_scandata.xml",
			"bytes": 5056,
			"selected": 1
		},
		{
			"id": 86,
			"path": "\/06-memory-management-1.mp3",
			"bytes": 11920091,
			"selected": 1
		},
		{
			"id": 87,
			"path": "\/06-memory-management-1.png",
			"bytes": 47143,
			"selected": 1
		},
		{
			"id": 88,
			"path": "\/06-memory-management-1_spectrogram.png",
			"bytes": 282335,
			"selected": 1
		},
		{
			"id": 89,
			"path": "\/07-c-memory-management-2.pdf",
			"bytes": 176254,
			"selected": 1
		},
		{
			"id": 90,
			"path": "\/07-c-memory-management-2_chocr.html.gz",
			"bytes": 173943,
			"selected": 1
		},
		{
			"id": 91,
			"path": "\/07-c-memory-management-2_djvu.txt",
			"bytes": 12576,
			"selected": 1
		},
		{
			"id": 92,
			"path": "\/07-c-memory-management-2_djvu.xml",
			"bytes": 193722,
			"selected": 1
		},
		{
			"id": 93,
			"path": "\/07-c-memory-management-2_hocr.html",
			"bytes": 414609,
			"selected": 1
		},
		{
			"id": 94,
			"path": "\/07-c-memory-management-2_hocr_pageindex.json.gz",
			"bytes": 193,
			"selected": 1
		},
		{
			"id": 95,
			"path": "\/07-c-memory-management-2_hocr_searchtext.txt.gz",
			"bytes": 4181,
			"selected": 1
		},
		{
			"id": 96,
			"path": "\/07-c-memory-management-2_jp2.zip",
			"bytes": 3682929,
			"selected": 1
		},
		{
			"id": 97,
			"path": "\/07-c-memory-management-2_page_numbers.json",
			"bytes": 3100,
			"selected": 1
		},
		{
			"id": 98,
			"path": "\/07-c-memory-management-2_scandata.xml",
			"bytes": 5056,
			"selected": 1
		},
		{
			"id": 99,
			"path": "\/07-hilfinger-notes.pdf",
			"bytes": 228213,
			"selected": 1
		},
		{
			"id": 100,
			"path": "\/07-hilfinger-notes_chocr.html.gz",
			"bytes": 628453,
			"selected": 1
		},
		{
			"id": 101,
			"path": "\/07-hilfinger-notes_djvu.txt",
			"bytes": 47905,
			"selected": 1
		},
		{
			"id": 102,
			"path": "\/07-hilfinger-notes_djvu.xml",
			"bytes": 636426,
			"selected": 1
		},
		{
			"id": 103,
			"path": "\/07-hilfinger-notes_hocr.html",
			"bytes": 1227887,
			"selected": 1
		},
		{
			"id": 104,
			"path": "\/07-hilfinger-notes_hocr_pageindex.json.gz",
			"bytes": 289,
			"selected": 1
		},
		{
			"id": 105,
			"path": "\/07-hilfinger-notes_hocr_searchtext.txt.gz",
			"bytes": 16280,
			"selected": 1
		},
		{
			"id": 106,
			"path": "\/07-hilfinger-notes_jp2.zip",
			"bytes": 8941426,
			"selected": 1
		},
		{
			"id": 107,
			"path": "\/07-hilfinger-notes_page_numbers.json",
			"bytes": 4805,
			"selected": 1
		},
		{
			"id": 108,
			"path": "\/07-hilfinger-notes_scandata.xml",
			"bytes": 8386,
			"selected": 1
		},
		{
			"id": 109,
			"path": "\/07-memory-management-2.mp3",
			"bytes": 12020088,
			"selected": 1
		},
		{
			"id": 110,
			"path": "\/07-memory-management-2.png",
			"bytes": 55803,
			"selected": 1
		},
		{
			"id": 111,
			"path": "\/07-memory-management-2_spectrogram.png",
			"bytes": 276609,
			"selected": 1
		},
		{
			"id": 112,
			"path": "\/08-introduction-to-mips.mp3",
			"bytes": 11272045,
			"selected": 1
		},
		{
			"id": 113,
			"path": "\/08-introduction-to-mips.pdf",
			"bytes": 370451,
			"selected": 1
		},
		{
			"id": 114,
			"path": "\/08-introduction-to-mips.png",
			"bytes": 58576,
			"selected": 1
		},
		{
			"id": 115,
			"path": "\/08-introduction-to-mips_chocr.html.gz",
			"bytes": 154612,
			"selected": 1
		},
		{
			"id": 116,
			"path": "\/08-introduction-to-mips_djvu.txt",
			"bytes": 11065,
			"selected": 1
		},
		{
			"id": 117,
			"path": "\/08-introduction-to-mips_djvu.xml",
			"bytes": 166083,
			"selected": 1
		},
		{
			"id": 118,
			"path": "\/08-introduction-to-mips_hocr.html",
			"bytes": 355239,
			"selected": 1
		},
		{
			"id": 119,
			"path": "\/08-introduction-to-mips_hocr_pageindex.json.gz",
			"bytes": 159,
			"selected": 1
		},
		{
			"id": 120,
			"path": "\/08-introduction-to-mips_hocr_searchtext.txt.gz",
			"bytes": 4033,
			"selected": 1
		},
		{
			"id": 121,
			"path": "\/08-introduction-to-mips_jp2.zip",
			"bytes": 3011014,
			"selected": 1
		},
		{
			"id": 122,
			"path": "\/08-introduction-to-mips_page_numbers.json",
			"bytes": 2367,
			"selected": 1
		},
		{
			"id": 123,
			"path": "\/08-introduction-to-mips_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 124,
			"path": "\/08-introduction-to-mips_spectrogram.png",
			"bytes": 273090,
			"selected": 1
		},
		{
			"id": 125,
			"path": "\/09-mips-load-store-and-branch-instructions-1.mp3",
			"bytes": 11200157,
			"selected": 1
		},
		{
			"id": 126,
			"path": "\/09-mips-load-store-and-branch-instructions-1.pdf",
			"bytes": 173997,
			"selected": 1
		},
		{
			"id": 127,
			"path": "\/09-mips-load-store-and-branch-instructions-1.png",
			"bytes": 55009,
			"selected": 1
		},
		{
			"id": 128,
			"path": "\/09-mips-load-store-and-branch-instructions-1_chocr.html.gz",
			"bytes": 140652,
			"selected": 1
		},
		{
			"id": 129,
			"path": "\/09-mips-load-store-and-branch-instructions-1_djvu.txt",
			"bytes": 10021,
			"selected": 1
		},
		{
			"id": 130,
			"path": "\/09-mips-load-store-and-branch-instructions-1_djvu.xml",
			"bytes": 150532,
			"selected": 1
		},
		{
			"id": 131,
			"path": "\/09-mips-load-store-and-branch-instructions-1_hocr.html",
			"bytes": 311528,
			"selected": 1
		},
		{
			"id": 132,
			"path": "\/09-mips-load-store-and-branch-instructions-1_hocr_pageindex.json.gz",
			"bytes": 149,
			"selected": 1
		},
		{
			"id": 133,
			"path": "\/09-mips-load-store-and-branch-instructions-1_hocr_searchtext.txt.gz",
			"bytes": 3520,
			"selected": 1
		},
		{
			"id": 134,
			"path": "\/09-mips-load-store-and-branch-instructions-1_jp2.zip",
			"bytes": 2921106,
			"selected": 1
		},
		{
			"id": 135,
			"path": "\/09-mips-load-store-and-branch-instructions-1_page_numbers.json",
			"bytes": 2294,
			"selected": 1
		},
		{
			"id": 136,
			"path": "\/09-mips-load-store-and-branch-instructions-1_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 137,
			"path": "\/09-mips-load-store-and-branch-instructions-1_spectrogram.png",
			"bytes": 280568,
			"selected": 1
		},
		{
			"id": 138,
			"path": "\/10-mips-branch-instructions-2.mp3",
			"bytes": 12060107,
			"selected": 1
		},
		{
			"id": 139,
			"path": "\/10-mips-branch-instructions-2.pdf",
			"bytes": 189326,
			"selected": 1
		},
		{
			"id": 140,
			"path": "\/10-mips-branch-instructions-2.png",
			"bytes": 48816,
			"selected": 1
		},
		{
			"id": 141,
			"path": "\/10-mips-branch-instructions-2_chocr.html.gz",
			"bytes": 130492,
			"selected": 1
		},
		{
			"id": 142,
			"path": "\/10-mips-branch-instructions-2_djvu.txt",
			"bytes": 9199,
			"selected": 1
		},
		{
			"id": 143,
			"path": "\/10-mips-branch-instructions-2_djvu.xml",
			"bytes": 144727,
			"selected": 1
		},
		{
			"id": 144,
			"path": "\/10-mips-branch-instructions-2_hocr.html",
			"bytes": 304203,
			"selected": 1
		},
		{
			"id": 145,
			"path": "\/10-mips-branch-instructions-2_hocr_pageindex.json.gz",
			"bytes": 161,
			"selected": 1
		},
		{
			"id": 146,
			"path": "\/10-mips-branch-instructions-2_hocr_searchtext.txt.gz",
			"bytes": 3421,
			"selected": 1
		},
		{
			"id": 147,
			"path": "\/10-mips-branch-instructions-2_jp2.zip",
			"bytes": 2819387,
			"selected": 1
		},
		{
			"id": 148,
			"path": "\/10-mips-branch-instructions-2_page_numbers.json",
			"bytes": 2405,
			"selected": 1
		},
		{
			"id": 149,
			"path": "\/10-mips-branch-instructions-2_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 150,
			"path": "\/10-mips-branch-instructions-2_spectrogram.png",
			"bytes": 282412,
			"selected": 1
		},
		{
			"id": 151,
			"path": "\/11-mips-procedures-1.mp3",
			"bytes": 10760150,
			"selected": 1
		},
		{
			"id": 152,
			"path": "\/11-mips-procedures-1.pdf",
			"bytes": 155337,
			"selected": 1
		},
		{
			"id": 153,
			"path": "\/11-mips-procedures-1.png",
			"bytes": 46061,
			"selected": 1
		},
		{
			"id": 154,
			"path": "\/11-mips-procedures-1_chocr.html.gz",
			"bytes": 115340,
			"selected": 1
		},
		{
			"id": 155,
			"path": "\/11-mips-procedures-1_djvu.txt",
			"bytes": 8108,
			"selected": 1
		},
		{
			"id": 156,
			"path": "\/11-mips-procedures-1_djvu.xml",
			"bytes": 125435,
			"selected": 1
		},
		{
			"id": 157,
			"path": "\/11-mips-procedures-1_hocr.html",
			"bytes": 265512,
			"selected": 1
		},
		{
			"id": 158,
			"path": "\/11-mips-procedures-1_hocr_pageindex.json.gz",
			"bytes": 149,
			"selected": 1
		},
		{
			"id": 159,
			"path": "\/11-mips-procedures-1_hocr_searchtext.txt.gz",
			"bytes": 2976,
			"selected": 1
		},
		{
			"id": 160,
			"path": "\/11-mips-procedures-1_jp2.zip",
			"bytes": 2513315,
			"selected": 1
		},
		{
			"id": 161,
			"path": "\/11-mips-procedures-1_page_numbers.json",
			"bytes": 2180,
			"selected": 1
		},
		{
			"id": 162,
			"path": "\/11-mips-procedures-1_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 163,
			"path": "\/11-mips-procedures-1_spectrogram.png",
			"bytes": 278796,
			"selected": 1
		},
		{
			"id": 164,
			"path": "\/12-mips-procedures-2-and-logical-ops.mp3",
			"bytes": 11736085,
			"selected": 1
		},
		{
			"id": 165,
			"path": "\/12-mips-procedures-2-and-logical-ops.pdf",
			"bytes": 174990,
			"selected": 1
		},
		{
			"id": 166,
			"path": "\/12-mips-procedures-2-and-logical-ops.png",
			"bytes": 45855,
			"selected": 1
		},
		{
			"id": 167,
			"path": "\/12-mips-procedures-2-and-logical-ops_chocr.html.gz",
			"bytes": 192437,
			"selected": 1
		},
		{
			"id": 168,
			"path": "\/12-mips-procedures-2-and-logical-ops_djvu.txt",
			"bytes": 13874,
			"selected": 1
		},
		{
			"id": 169,
			"path": "\/12-mips-procedures-2-and-logical-ops_djvu.xml",
			"bytes": 216029,
			"selected": 1
		},
		{
			"id": 170,
			"path": "\/12-mips-procedures-2-and-logical-ops_hocr.html",
			"bytes": 440053,
			"selected": 1
		},
		{
			"id": 171,
			"path": "\/12-mips-procedures-2-and-logical-ops_hocr_pageindex.json.gz",
			"bytes": 202,
			"selected": 1
		},
		{
			"id": 172,
			"path": "\/12-mips-procedures-2-and-logical-ops_hocr_searchtext.txt.gz",
			"bytes": 4430,
			"selected": 1
		},
		{
			"id": 173,
			"path": "\/12-mips-procedures-2-and-logical-ops_jp2.zip",
			"bytes": 3851680,
			"selected": 1
		},
		{
			"id": 174,
			"path": "\/12-mips-procedures-2-and-logical-ops_page_numbers.json",
			"bytes": 3087,
			"selected": 1
		},
		{
			"id": 175,
			"path": "\/12-mips-procedures-2-and-logical-ops_scandata.xml",
			"bytes": 5376,
			"selected": 1
		},
		{
			"id": 176,
			"path": "\/12-mips-procedures-2-and-logical-ops_spectrogram.png",
			"bytes": 282876,
			"selected": 1
		},
		{
			"id": 177,
			"path": "\/13-mips-instruction-representation-1.mp3",
			"bytes": 11896164,
			"selected": 1
		},
		{
			"id": 178,
			"path": "\/13-mips-instruction-representation-1.pdf",
			"bytes": 154599,
			"selected": 1
		},
		{
			"id": 179,
			"path": "\/13-mips-instruction-representation-1.png",
			"bytes": 46806,
			"selected": 1
		},
		{
			"id": 180,
			"path": "\/13-mips-instruction-representation-1_chocr.html.gz",
			"bytes": 155594,
			"selected": 1
		},
		{
			"id": 181,
			"path": "\/13-mips-instruction-representation-1_djvu.txt",
			"bytes": 11269,
			"selected": 1
		},
		{
			"id": 182,
			"path": "\/13-mips-instruction-representation-1_djvu.xml",
			"bytes": 162752,
			"selected": 1
		},
		{
			"id": 183,
			"path": "\/13-mips-instruction-representation-1_hocr.html",
			"bytes": 334673,
			"selected": 1
		},
		{
			"id": 184,
			"path": "\/13-mips-instruction-representation-1_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 185,
			"path": "\/13-mips-instruction-representation-1_hocr_searchtext.txt.gz",
			"bytes": 3871,
			"selected": 1
		},
		{
			"id": 186,
			"path": "\/13-mips-instruction-representation-1_jp2.zip",
			"bytes": 3068814,
			"selected": 1
		},
		{
			"id": 187,
			"path": "\/13-mips-instruction-representation-1_page_numbers.json",
			"bytes": 2328,
			"selected": 1
		},
		{
			"id": 188,
			"path": "\/13-mips-instruction-representation-1_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 189,
			"path": "\/13-mips-instruction-representation-1_spectrogram.png",
			"bytes": 277415,
			"selected": 1
		},
		{
			"id": 190,
			"path": "\/14-mips-instruction-representation-2.mp3",
			"bytes": 11652075,
			"selected": 1
		},
		{
			"id": 191,
			"path": "\/14-mips-instruction-representation-2.pdf",
			"bytes": 134987,
			"selected": 1
		},
		{
			"id": 192,
			"path": "\/14-mips-instruction-representation-2.png",
			"bytes": 27689,
			"selected": 1
		},
		{
			"id": 193,
			"path": "\/14-mips-instruction-representation-2_chocr.html.gz",
			"bytes": 128551,
			"selected": 1
		},
		{
			"id": 194,
			"path": "\/14-mips-instruction-representation-2_djvu.txt",
			"bytes": 9141,
			"selected": 1
		},
		{
			"id": 195,
			"path": "\/14-mips-instruction-representation-2_djvu.xml",
			"bytes": 142622,
			"selected": 1
		},
		{
			"id": 196,
			"path": "\/14-mips-instruction-representation-2_hocr.html",
			"bytes": 313611,
			"selected": 1
		},
		{
			"id": 197,
			"path": "\/14-mips-instruction-representation-2_hocr_pageindex.json.gz",
			"bytes": 152,
			"selected": 1
		},
		{
			"id": 198,
			"path": "\/14-mips-instruction-representation-2_hocr_searchtext.txt.gz",
			"bytes": 3080,
			"selected": 1
		},
		{
			"id": 199,
			"path": "\/14-mips-instruction-representation-2_jp2.zip",
			"bytes": 2543066,
			"selected": 1
		},
		{
			"id": 200,
			"path": "\/14-mips-instruction-representation-2_page_numbers.json",
			"bytes": 2123,
			"selected": 1
		},
		{
			"id": 201,
			"path": "\/14-mips-instruction-representation-2_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 202,
			"path": "\/14-mips-instruction-representation-2_spectrogram.png",
			"bytes": 140386,
			"selected": 1
		},
		{
			"id": 203,
			"path": "\/15-floating-point-1.mp3",
			"bytes": 11932108,
			"selected": 1
		},
		{
			"id": 204,
			"path": "\/15-floating-point-1.pdf",
			"bytes": 138859,
			"selected": 1
		},
		{
			"id": 205,
			"path": "\/15-floating-point-1.png",
			"bytes": 63749,
			"selected": 1
		},
		{
			"id": 206,
			"path": "\/15-floating-point-1_chocr.html.gz",
			"bytes": 153574,
			"selected": 1
		},
		{
			"id": 207,
			"path": "\/15-floating-point-1_djvu.txt",
			"bytes": 11093,
			"selected": 1
		},
		{
			"id": 208,
			"path": "\/15-floating-point-1_djvu.xml",
			"bytes": 162527,
			"selected": 1
		},
		{
			"id": 209,
			"path": "\/15-floating-point-1_hocr.html",
			"bytes": 339265,
			"selected": 1
		},
		{
			"id": 210,
			"path": "\/15-floating-point-1_hocr_pageindex.json.gz",
			"bytes": 172,
			"selected": 1
		},
		{
			"id": 211,
			"path": "\/15-floating-point-1_hocr_searchtext.txt.gz",
			"bytes": 4235,
			"selected": 1
		},
		{
			"id": 212,
			"path": "\/15-floating-point-1_jp2.zip",
			"bytes": 3144656,
			"selected": 1
		},
		{
			"id": 213,
			"path": "\/15-floating-point-1_page_numbers.json",
			"bytes": 2635,
			"selected": 1
		},
		{
			"id": 214,
			"path": "\/15-floating-point-1_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 215,
			"path": "\/15-floating-point-1_spectrogram.png",
			"bytes": 272107,
			"selected": 1
		},
		{
			"id": 216,
			"path": "\/16-floating-point-2.mp3",
			"bytes": 12052062,
			"selected": 1
		},
		{
			"id": 217,
			"path": "\/16-floating-point-2.pdf",
			"bytes": 179447,
			"selected": 1
		},
		{
			"id": 218,
			"path": "\/16-floating-point-2.png",
			"bytes": 47084,
			"selected": 1
		},
		{
			"id": 219,
			"path": "\/16-floating-point-2_chocr.html.gz",
			"bytes": 149565,
			"selected": 1
		},
		{
			"id": 220,
			"path": "\/16-floating-point-2_djvu.txt",
			"bytes": 10716,
			"selected": 1
		},
		{
			"id": 221,
			"path": "\/16-floating-point-2_djvu.xml",
			"bytes": 160540,
			"selected": 1
		},
		{
			"id": 222,
			"path": "\/16-floating-point-2_hocr.html",
			"bytes": 342927,
			"selected": 1
		},
		{
			"id": 223,
			"path": "\/16-floating-point-2_hocr_pageindex.json.gz",
			"bytes": 168,
			"selected": 1
		},
		{
			"id": 224,
			"path": "\/16-floating-point-2_hocr_searchtext.txt.gz",
			"bytes": 3989,
			"selected": 1
		},
		{
			"id": 225,
			"path": "\/16-floating-point-2_jp2.zip",
			"bytes": 3086132,
			"selected": 1
		},
		{
			"id": 226,
			"path": "\/16-floating-point-2_page_numbers.json",
			"bytes": 2677,
			"selected": 1
		},
		{
			"id": 227,
			"path": "\/16-floating-point-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 228,
			"path": "\/16-floating-point-2_spectrogram.png",
			"bytes": 280689,
			"selected": 1
		},
		{
			"id": 229,
			"path": "\/17-mips-instruction-representation-3.mp3",
			"bytes": 10464131,
			"selected": 1
		},
		{
			"id": 230,
			"path": "\/17-mips-instruction-representation-3.pdf",
			"bytes": 147749,
			"selected": 1
		},
		{
			"id": 231,
			"path": "\/17-mips-instruction-representation-3.png",
			"bytes": 47974,
			"selected": 1
		},
		{
			"id": 232,
			"path": "\/17-mips-instruction-representation-3_chocr.html.gz",
			"bytes": 154506,
			"selected": 1
		},
		{
			"id": 233,
			"path": "\/17-mips-instruction-representation-3_djvu.txt",
			"bytes": 11021,
			"selected": 1
		},
		{
			"id": 234,
			"path": "\/17-mips-instruction-representation-3_djvu.xml",
			"bytes": 159486,
			"selected": 1
		},
		{
			"id": 235,
			"path": "\/17-mips-instruction-representation-3_hocr.html",
			"bytes": 353572,
			"selected": 1
		},
		{
			"id": 236,
			"path": "\/17-mips-instruction-representation-3_hocr_pageindex.json.gz",
			"bytes": 182,
			"selected": 1
		},
		{
			"id": 237,
			"path": "\/17-mips-instruction-representation-3_hocr_searchtext.txt.gz",
			"bytes": 3615,
			"selected": 1
		},
		{
			"id": 238,
			"path": "\/17-mips-instruction-representation-3_jp2.zip",
			"bytes": 3151958,
			"selected": 1
		},
		{
			"id": 239,
			"path": "\/17-mips-instruction-representation-3_page_numbers.json",
			"bytes": 2914,
			"selected": 1
		},
		{
			"id": 240,
			"path": "\/17-mips-instruction-representation-3_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 241,
			"path": "\/17-mips-instruction-representation-3_spectrogram.png",
			"bytes": 284554,
			"selected": 1
		},
		{
			"id": 242,
			"path": "\/18-compilation-assembly-linking-1.mp3",
			"bytes": 10912078,
			"selected": 1
		},
		{
			"id": 243,
			"path": "\/18-compilation-assembly-linking-1.pdf",
			"bytes": 152998,
			"selected": 1
		},
		{
			"id": 244,
			"path": "\/18-compilation-assembly-linking-1.png",
			"bytes": 46914,
			"selected": 1
		},
		{
			"id": 245,
			"path": "\/18-compilation-assembly-linking-1_chocr.html.gz",
			"bytes": 140596,
			"selected": 1
		},
		{
			"id": 246,
			"path": "\/18-compilation-assembly-linking-1_djvu.txt",
			"bytes": 10074,
			"selected": 1
		},
		{
			"id": 247,
			"path": "\/18-compilation-assembly-linking-1_djvu.xml",
			"bytes": 147878,
			"selected": 1
		},
		{
			"id": 248,
			"path": "\/18-compilation-assembly-linking-1_hocr.html",
			"bytes": 312826,
			"selected": 1
		},
		{
			"id": 249,
			"path": "\/18-compilation-assembly-linking-1_hocr_pageindex.json.gz",
			"bytes": 171,
			"selected": 1
		},
		{
			"id": 250,
			"path": "\/18-compilation-assembly-linking-1_hocr_searchtext.txt.gz",
			"bytes": 3681,
			"selected": 1
		},
		{
			"id": 251,
			"path": "\/18-compilation-assembly-linking-1_jp2.zip",
			"bytes": 3008609,
			"selected": 1
		},
		{
			"id": 252,
			"path": "\/18-compilation-assembly-linking-1_page_numbers.json",
			"bytes": 2586,
			"selected": 1
		},
		{
			"id": 253,
			"path": "\/18-compilation-assembly-linking-1_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 254,
			"path": "\/18-compilation-assembly-linking-1_spectrogram.png",
			"bytes": 284690,
			"selected": 1
		},
		{
			"id": 255,
			"path": "\/19-compilation-assembly-linking-2.mp3",
			"bytes": 11972127,
			"selected": 1
		},
		{
			"id": 256,
			"path": "\/19-compilation-assembly-linking-2.pdf",
			"bytes": 188893,
			"selected": 1
		},
		{
			"id": 257,
			"path": "\/19-compilation-assembly-linking-2.png",
			"bytes": 47278,
			"selected": 1
		},
		{
			"id": 258,
			"path": "\/19-compilation-assembly-linking-2_chocr.html.gz",
			"bytes": 203397,
			"selected": 1
		},
		{
			"id": 259,
			"path": "\/19-compilation-assembly-linking-2_djvu.txt",
			"bytes": 15056,
			"selected": 1
		},
		{
			"id": 260,
			"path": "\/19-compilation-assembly-linking-2_djvu.xml",
			"bytes": 221423,
			"selected": 1
		},
		{
			"id": 261,
			"path": "\/19-compilation-assembly-linking-2_hocr.html",
			"bytes": 478838,
			"selected": 1
		},
		{
			"id": 262,
			"path": "\/19-compilation-assembly-linking-2_hocr_pageindex.json.gz",
			"bytes": 211,
			"selected": 1
		},
		{
			"id": 263,
			"path": "\/19-compilation-assembly-linking-2_hocr_searchtext.txt.gz",
			"bytes": 5188,
			"selected": 1
		},
		{
			"id": 264,
			"path": "\/19-compilation-assembly-linking-2_jp2.zip",
			"bytes": 4371369,
			"selected": 1
		},
		{
			"id": 265,
			"path": "\/19-compilation-assembly-linking-2_page_numbers.json",
			"bytes": 3420,
			"selected": 1
		},
		{
			"id": 266,
			"path": "\/19-compilation-assembly-linking-2_scandata.xml",
			"bytes": 5696,
			"selected": 1
		},
		{
			"id": 267,
			"path": "\/19-compilation-assembly-linking-2_spectrogram.png",
			"bytes": 278569,
			"selected": 1
		},
		{
			"id": 268,
			"path": "\/20-introduction-to-synchronous-digital-systems.mp3",
			"bytes": 12172121,
			"selected": 1
		},
		{
			"id": 269,
			"path": "\/20-introduction-to-synchronous-digital-systems.pdf",
			"bytes": 2679520,
			"selected": 1
		},
		{
			"id": 270,
			"path": "\/20-introduction-to-synchronous-digital-systems.png",
			"bytes": 47523,
			"selected": 1
		},
		{
			"id": 271,
			"path": "\/20-introduction-to-synchronous-digital-systems_chocr.html.gz",
			"bytes": 65973,
			"selected": 1
		},
		{
			"id": 272,
			"path": "\/20-introduction-to-synchronous-digital-systems_djvu.txt",
			"bytes": 4676,
			"selected": 1
		},
		{
			"id": 273,
			"path": "\/20-introduction-to-synchronous-digital-systems_djvu.xml",
			"bytes": 74142,
			"selected": 1
		},
		{
			"id": 274,
			"path": "\/20-introduction-to-synchronous-digital-systems_hocr.html",
			"bytes": 161692,
			"selected": 1
		},
		{
			"id": 275,
			"path": "\/20-introduction-to-synchronous-digital-systems_hocr_pageindex.json.gz",
			"bytes": 113,
			"selected": 1
		},
		{
			"id": 276,
			"path": "\/20-introduction-to-synchronous-digital-systems_hocr_searchtext.txt.gz",
			"bytes": 1898,
			"selected": 1
		},
		{
			"id": 277,
			"path": "\/20-introduction-to-synchronous-digital-systems_jp2.zip",
			"bytes": 1495323,
			"selected": 1
		},
		{
			"id": 278,
			"path": "\/20-introduction-to-synchronous-digital-systems_page_numbers.json",
			"bytes": 1447,
			"selected": 1
		},
		{
			"id": 279,
			"path": "\/20-introduction-to-synchronous-digital-systems_scandata.xml",
			"bytes": 2499,
			"selected": 1
		},
		{
			"id": 280,
			"path": "\/20-introduction-to-synchronous-digital-systems_spectrogram.png",
			"bytes": 279328,
			"selected": 1
		},
		{
			"id": 281,
			"path": "\/21-state-elements.mp3",
			"bytes": 11892088,
			"selected": 1
		},
		{
			"id": 282,
			"path": "\/21-state-elements.pdf",
			"bytes": 1877963,
			"selected": 1
		},
		{
			"id": 283,
			"path": "\/21-state-elements.png",
			"bytes": 45450,
			"selected": 1
		},
		{
			"id": 284,
			"path": "\/21-state-elements_chocr.html.gz",
			"bytes": 107863,
			"selected": 1
		},
		{
			"id": 285,
			"path": "\/21-state-elements_djvu.txt",
			"bytes": 7641,
			"selected": 1
		},
		{
			"id": 286,
			"path": "\/21-state-elements_djvu.xml",
			"bytes": 121193,
			"selected": 1
		},
		{
			"id": 287,
			"path": "\/21-state-elements_hocr.html",
			"bytes": 259942,
			"selected": 1
		},
		{
			"id": 288,
			"path": "\/21-state-elements_hocr_pageindex.json.gz",
			"bytes": 143,
			"selected": 1
		},
		{
			"id": 289,
			"path": "\/21-state-elements_hocr_searchtext.txt.gz",
			"bytes": 2810,
			"selected": 1
		},
		{
			"id": 290,
			"path": "\/21-state-elements_jp2.zip",
			"bytes": 2307995,
			"selected": 1
		},
		{
			"id": 291,
			"path": "\/21-state-elements_page_numbers.json",
			"bytes": 1998,
			"selected": 1
		},
		{
			"id": 292,
			"path": "\/21-state-elements_scandata.xml",
			"bytes": 3456,
			"selected": 1
		},
		{
			"id": 293,
			"path": "\/21-state-elements_spectrogram.png",
			"bytes": 281151,
			"selected": 1
		},
		{
			"id": 294,
			"path": "\/22-boolean-logic.pdf",
			"bytes": 361367,
			"selected": 1
		},
		{
			"id": 295,
			"path": "\/22-boolean-logic_chocr.html.gz",
			"bytes": 184873,
			"selected": 1
		},
		{
			"id": 296,
			"path": "\/22-boolean-logic_djvu.txt",
			"bytes": 14121,
			"selected": 1
		},
		{
			"id": 297,
			"path": "\/22-boolean-logic_djvu.xml",
			"bytes": 201107,
			"selected": 1
		},
		{
			"id": 298,
			"path": "\/22-boolean-logic_hocr.html",
			"bytes": 394012,
			"selected": 1
		},
		{
			"id": 299,
			"path": "\/22-boolean-logic_hocr_pageindex.json.gz",
			"bytes": 156,
			"selected": 1
		},
		{
			"id": 300,
			"path": "\/22-boolean-logic_hocr_searchtext.txt.gz",
			"bytes": 5221,
			"selected": 1
		},
		{
			"id": 301,
			"path": "\/22-boolean-logic_jp2.zip",
			"bytes": 3061798,
			"selected": 1
		},
		{
			"id": 302,
			"path": "\/22-boolean-logic_page_numbers.json",
			"bytes": 2145,
			"selected": 1
		},
		{
			"id": 303,
			"path": "\/22-boolean-logic_scandata.xml",
			"bytes": 4108,
			"selected": 1
		},
		{
			"id": 304,
			"path": "\/22-combinational-logic-1.mp3",
			"bytes": 9220075,
			"selected": 1
		},
		{
			"id": 305,
			"path": "\/22-combinational-logic-1.png",
			"bytes": 44996,
			"selected": 1
		},
		{
			"id": 306,
			"path": "\/22-combinational-logic-1_spectrogram.png",
			"bytes": 280242,
			"selected": 1
		},
		{
			"id": 307,
			"path": "\/22-combinational-logic-2.mp3",
			"bytes": 12326974,
			"selected": 1
		},
		{
			"id": 308,
			"path": "\/22-combinational-logic-2.png",
			"bytes": 48753,
			"selected": 1
		},
		{
			"id": 309,
			"path": "\/22-combinational-logic-2_spectrogram.png",
			"bytes": 277469,
			"selected": 1
		},
		{
			"id": 310,
			"path": "\/22-combinational-logic.pdf",
			"bytes": 1430968,
			"selected": 1
		},
		{
			"id": 311,
			"path": "\/22-combinational-logic_chocr.html.gz",
			"bytes": 80349,
			"selected": 1
		},
		{
			"id": 312,
			"path": "\/22-combinational-logic_djvu.txt",
			"bytes": 5435,
			"selected": 1
		},
		{
			"id": 313,
			"path": "\/22-combinational-logic_djvu.xml",
			"bytes": 106211,
			"selected": 1
		},
		{
			"id": 314,
			"path": "\/22-combinational-logic_hocr.html",
			"bytes": 299001,
			"selected": 1
		},
		{
			"id": 315,
			"path": "\/22-combinational-logic_hocr_pageindex.json.gz",
			"bytes": 174,
			"selected": 1
		},
		{
			"id": 316,
			"path": "\/22-combinational-logic_hocr_searchtext.txt.gz",
			"bytes": 2155,
			"selected": 1
		},
		{
			"id": 317,
			"path": "\/22-combinational-logic_jp2.zip",
			"bytes": 3859950,
			"selected": 1
		},
		{
			"id": 318,
			"path": "\/22-combinational-logic_page_numbers.json",
			"bytes": 2269,
			"selected": 1
		},
		{
			"id": 319,
			"path": "\/22-combinational-logic_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 320,
			"path": "\/23-blocks.pdf",
			"bytes": 1144358,
			"selected": 1
		},
		{
			"id": 321,
			"path": "\/23-blocks_chocr.html.gz",
			"bytes": 72751,
			"selected": 1
		},
		{
			"id": 322,
			"path": "\/23-blocks_djvu.txt",
			"bytes": 5179,
			"selected": 1
		},
		{
			"id": 323,
			"path": "\/23-blocks_djvu.xml",
			"bytes": 88926,
			"selected": 1
		},
		{
			"id": 324,
			"path": "\/23-blocks_hocr.html",
			"bytes": 196615,
			"selected": 1
		},
		{
			"id": 325,
			"path": "\/23-blocks_hocr_pageindex.json.gz",
			"bytes": 142,
			"selected": 1
		},
		{
			"id": 326,
			"path": "\/23-blocks_hocr_searchtext.txt.gz",
			"bytes": 1927,
			"selected": 1
		},
		{
			"id": 327,
			"path": "\/23-blocks_jp2.zip",
			"bytes": 1574079,
			"selected": 1
		},
		{
			"id": 328,
			"path": "\/23-blocks_page_numbers.json",
			"bytes": 2044,
			"selected": 1
		},
		{
			"id": 329,
			"path": "\/23-blocks_scandata.xml",
			"bytes": 3456,
			"selected": 1
		},
		{
			"id": 330,
			"path": "\/23-combinational-logic-blocks-1.mp3",
			"bytes": 11992085,
			"selected": 1
		},
		{
			"id": 331,
			"path": "\/23-combinational-logic-blocks-1.pdf",
			"bytes": 471837,
			"selected": 1
		},
		{
			"id": 332,
			"path": "\/23-combinational-logic-blocks-1.png",
			"bytes": 47665,
			"selected": 1
		},
		{
			"id": 333,
			"path": "\/23-combinational-logic-blocks-1_chocr.html.gz",
			"bytes": 188721,
			"selected": 1
		},
		{
			"id": 334,
			"path": "\/23-combinational-logic-blocks-1_djvu.txt",
			"bytes": 14548,
			"selected": 1
		},
		{
			"id": 335,
			"path": "\/23-combinational-logic-blocks-1_djvu.xml",
			"bytes": 214648,
			"selected": 1
		},
		{
			"id": 336,
			"path": "\/23-combinational-logic-blocks-1_hocr.html",
			"bytes": 402819,
			"selected": 1
		},
		{
			"id": 337,
			"path": "\/23-combinational-logic-blocks-1_hocr_pageindex.json.gz",
			"bytes": 126,
			"selected": 1
		},
		{
			"id": 338,
			"path": "\/23-combinational-logic-blocks-1_hocr_searchtext.txt.gz",
			"bytes": 5536,
			"selected": 1
		},
		{
			"id": 339,
			"path": "\/23-combinational-logic-blocks-1_jp2.zip",
			"bytes": 3148140,
			"selected": 1
		},
		{
			"id": 340,
			"path": "\/23-combinational-logic-blocks-1_page_numbers.json",
			"bytes": 1550,
			"selected": 1
		},
		{
			"id": 341,
			"path": "\/23-combinational-logic-blocks-1_scandata.xml",
			"bytes": 3048,
			"selected": 1
		},
		{
			"id": 342,
			"path": "\/23-combinational-logic-blocks-1_spectrogram.png",
			"bytes": 280292,
			"selected": 1
		},
		{
			"id": 343,
			"path": "\/24-blocks.pdf",
			"bytes": 1144358,
			"selected": 1
		},
		{
			"id": 344,
			"path": "\/24-blocks_chocr.html.gz",
			"bytes": 72192,
			"selected": 1
		},
		{
			"id": 345,
			"path": "\/24-blocks_djvu.txt",
			"bytes": 5178,
			"selected": 1
		},
		{
			"id": 346,
			"path": "\/24-blocks_djvu.xml",
			"bytes": 89113,
			"selected": 1
		},
		{
			"id": 347,
			"path": "\/24-blocks_hocr.html",
			"bytes": 196098,
			"selected": 1
		},
		{
			"id": 348,
			"path": "\/24-blocks_hocr_pageindex.json.gz",
			"bytes": 140,
			"selected": 1
		},
		{
			"id": 349,
			"path": "\/24-blocks_hocr_searchtext.txt.gz",
			"bytes": 1928,
			"selected": 1
		},
		{
			"id": 350,
			"path": "\/24-blocks_jp2.zip",
			"bytes": 1574079,
			"selected": 1
		},
		{
			"id": 351,
			"path": "\/24-blocks_page_numbers.json",
			"bytes": 2065,
			"selected": 1
		},
		{
			"id": 352,
			"path": "\/24-blocks_scandata.xml",
			"bytes": 3456,
			"selected": 1
		},
		{
			"id": 353,
			"path": "\/24-combinational-logic-blocks-2.mp3",
			"bytes": 10020049,
			"selected": 1
		},
		{
			"id": 354,
			"path": "\/24-combinational-logic-blocks-2.pdf",
			"bytes": 471837,
			"selected": 1
		},
		{
			"id": 355,
			"path": "\/24-combinational-logic-blocks-2.png",
			"bytes": 43870,
			"selected": 1
		},
		{
			"id": 356,
			"path": "\/24-combinational-logic-blocks-2_chocr.html.gz",
			"bytes": 188721,
			"selected": 1
		},
		{
			"id": 357,
			"path": "\/24-combinational-logic-blocks-2_djvu.txt",
			"bytes": 14548,
			"selected": 1
		},
		{
			"id": 358,
			"path": "\/24-combinational-logic-blocks-2_djvu.xml",
			"bytes": 214648,
			"selected": 1
		},
		{
			"id": 359,
			"path": "\/24-combinational-logic-blocks-2_hocr.html",
			"bytes": 402819,
			"selected": 1
		},
		{
			"id": 360,
			"path": "\/24-combinational-logic-blocks-2_hocr_pageindex.json.gz",
			"bytes": 126,
			"selected": 1
		},
		{
			"id": 361,
			"path": "\/24-combinational-logic-blocks-2_hocr_searchtext.txt.gz",
			"bytes": 5536,
			"selected": 1
		},
		{
			"id": 362,
			"path": "\/24-combinational-logic-blocks-2_jp2.zip",
			"bytes": 3148140,
			"selected": 1
		},
		{
			"id": 363,
			"path": "\/24-combinational-logic-blocks-2_page_numbers.json",
			"bytes": 1550,
			"selected": 1
		},
		{
			"id": 364,
			"path": "\/24-combinational-logic-blocks-2_scandata.xml",
			"bytes": 3048,
			"selected": 1
		},
		{
			"id": 365,
			"path": "\/24-combinational-logic-blocks-2_spectrogram.png",
			"bytes": 290366,
			"selected": 1
		},
		{
			"id": 366,
			"path": "\/25-cpu-datapath-design-1.mp3",
			"bytes": 11460128,
			"selected": 1
		},
		{
			"id": 367,
			"path": "\/25-cpu-datapath-design-1.pdf",
			"bytes": 168314,
			"selected": 1
		},
		{
			"id": 368,
			"path": "\/25-cpu-datapath-design-1.png",
			"bytes": 47711,
			"selected": 1
		},
		{
			"id": 369,
			"path": "\/25-cpu-datapath-design-1_chocr.html.gz",
			"bytes": 129266,
			"selected": 1
		},
		{
			"id": 370,
			"path": "\/25-cpu-datapath-design-1_djvu.txt",
			"bytes": 9319,
			"selected": 1
		},
		{
			"id": 371,
			"path": "\/25-cpu-datapath-design-1_djvu.xml",
			"bytes": 145123,
			"selected": 1
		},
		{
			"id": 372,
			"path": "\/25-cpu-datapath-design-1_hocr.html",
			"bytes": 303557,
			"selected": 1
		},
		{
			"id": 373,
			"path": "\/25-cpu-datapath-design-1_hocr_pageindex.json.gz",
			"bytes": 163,
			"selected": 1
		},
		{
			"id": 374,
			"path": "\/25-cpu-datapath-design-1_hocr_searchtext.txt.gz",
			"bytes": 3027,
			"selected": 1
		},
		{
			"id": 375,
			"path": "\/25-cpu-datapath-design-1_jp2.zip",
			"bytes": 2810720,
			"selected": 1
		},
		{
			"id": 376,
			"path": "\/25-cpu-datapath-design-1_page_numbers.json",
			"bytes": 2316,
			"selected": 1
		},
		{
			"id": 377,
			"path": "\/25-cpu-datapath-design-1_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 378,
			"path": "\/25-cpu-datapath-design-1_spectrogram.png",
			"bytes": 277065,
			"selected": 1
		},
		{
			"id": 379,
			"path": "\/26-cpu-datapath-design-2.mp3",
			"bytes": 11560124,
			"selected": 1
		},
		{
			"id": 380,
			"path": "\/26-cpu-datapath-design-2.pdf",
			"bytes": 193759,
			"selected": 1
		},
		{
			"id": 381,
			"path": "\/26-cpu-datapath-design-2.png",
			"bytes": 46888,
			"selected": 1
		},
		{
			"id": 382,
			"path": "\/26-cpu-datapath-design-2_chocr.html.gz",
			"bytes": 124443,
			"selected": 1
		},
		{
			"id": 383,
			"path": "\/26-cpu-datapath-design-2_djvu.txt",
			"bytes": 8861,
			"selected": 1
		},
		{
			"id": 384,
			"path": "\/26-cpu-datapath-design-2_djvu.xml",
			"bytes": 140801,
			"selected": 1
		},
		{
			"id": 385,
			"path": "\/26-cpu-datapath-design-2_hocr.html",
			"bytes": 297669,
			"selected": 1
		},
		{
			"id": 386,
			"path": "\/26-cpu-datapath-design-2_hocr_pageindex.json.gz",
			"bytes": 151,
			"selected": 1
		},
		{
			"id": 387,
			"path": "\/26-cpu-datapath-design-2_hocr_searchtext.txt.gz",
			"bytes": 3095,
			"selected": 1
		},
		{
			"id": 388,
			"path": "\/26-cpu-datapath-design-2_jp2.zip",
			"bytes": 2528696,
			"selected": 1
		},
		{
			"id": 389,
			"path": "\/26-cpu-datapath-design-2_page_numbers.json",
			"bytes": 2231,
			"selected": 1
		},
		{
			"id": 390,
			"path": "\/26-cpu-datapath-design-2_scandata.xml",
			"bytes": 3776,
			"selected": 1
		},
		{
			"id": 391,
			"path": "\/26-cpu-datapath-design-2_spectrogram.png",
			"bytes": 282349,
			"selected": 1
		},
		{
			"id": 392,
			"path": "\/27-cpu-control-design-1.mp3",
			"bytes": 12280059,
			"selected": 1
		},
		{
			"id": 393,
			"path": "\/27-cpu-control-design-1.pdf",
			"bytes": 182070,
			"selected": 1
		},
		{
			"id": 394,
			"path": "\/27-cpu-control-design-1.png",
			"bytes": 47001,
			"selected": 1
		},
		{
			"id": 395,
			"path": "\/27-cpu-control-design-1_chocr.html.gz",
			"bytes": 100267,
			"selected": 1
		},
		{
			"id": 396,
			"path": "\/27-cpu-control-design-1_djvu.txt",
			"bytes": 7023,
			"selected": 1
		},
		{
			"id": 397,
			"path": "\/27-cpu-control-design-1_djvu.xml",
			"bytes": 117295,
			"selected": 1
		},
		{
			"id": 398,
			"path": "\/27-cpu-control-design-1_hocr.html",
			"bytes": 261807,
			"selected": 1
		},
		{
			"id": 399,
			"path": "\/27-cpu-control-design-1_hocr_pageindex.json.gz",
			"bytes": 132,
			"selected": 1
		},
		{
			"id": 400,
			"path": "\/27-cpu-control-design-1_hocr_searchtext.txt.gz",
			"bytes": 2389,
			"selected": 1
		},
		{
			"id": 401,
			"path": "\/27-cpu-control-design-1_jp2.zip",
			"bytes": 2245215,
			"selected": 1
		},
		{
			"id": 402,
			"path": "\/27-cpu-control-design-1_page_numbers.json",
			"bytes": 1796,
			"selected": 1
		},
		{
			"id": 403,
			"path": "\/27-cpu-control-design-1_scandata.xml",
			"bytes": 3136,
			"selected": 1
		},
		{
			"id": 404,
			"path": "\/27-cpu-control-design-1_spectrogram.png",
			"bytes": 280950,
			"selected": 1
		},
		{
			"id": 405,
			"path": "\/28-cpu-control-design-2.mp3",
			"bytes": 12060108,
			"selected": 1
		},
		{
			"id": 406,
			"path": "\/28-cpu-control-design-2.pdf",
			"bytes": 266356,
			"selected": 1
		},
		{
			"id": 407,
			"path": "\/28-cpu-control-design-2.png",
			"bytes": 47679,
			"selected": 1
		},
		{
			"id": 408,
			"path": "\/28-cpu-control-design-2_chocr.html.gz",
			"bytes": 143421,
			"selected": 1
		},
		{
			"id": 409,
			"path": "\/28-cpu-control-design-2_djvu.txt",
			"bytes": 9827,
			"selected": 1
		},
		{
			"id": 410,
			"path": "\/28-cpu-control-design-2_djvu.xml",
			"bytes": 173333,
			"selected": 1
		},
		{
			"id": 411,
			"path": "\/28-cpu-control-design-2_hocr.html",
			"bytes": 408764,
			"selected": 1
		},
		{
			"id": 412,
			"path": "\/28-cpu-control-design-2_hocr_pageindex.json.gz",
			"bytes": 167,
			"selected": 1
		},
		{
			"id": 413,
			"path": "\/28-cpu-control-design-2_hocr_searchtext.txt.gz",
			"bytes": 3100,
			"selected": 1
		},
		{
			"id": 414,
			"path": "\/28-cpu-control-design-2_jp2.zip",
			"bytes": 3374224,
			"selected": 1
		},
		{
			"id": 415,
			"path": "\/28-cpu-control-design-2_page_numbers.json",
			"bytes": 2663,
			"selected": 1
		},
		{
			"id": 416,
			"path": "\/28-cpu-control-design-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 417,
			"path": "\/28-cpu-control-design-2_spectrogram.png",
			"bytes": 277904,
			"selected": 1
		},
		{
			"id": 418,
			"path": "\/29-pipelining-1.mp3",
			"bytes": 11268075,
			"selected": 1
		},
		{
			"id": 419,
			"path": "\/29-pipelining-1.pdf",
			"bytes": 278732,
			"selected": 1
		},
		{
			"id": 420,
			"path": "\/29-pipelining-1.png",
			"bytes": 46949,
			"selected": 1
		},
		{
			"id": 421,
			"path": "\/29-pipelining-1_chocr.html.gz",
			"bytes": 144637,
			"selected": 1
		},
		{
			"id": 422,
			"path": "\/29-pipelining-1_djvu.txt",
			"bytes": 10346,
			"selected": 1
		},
		{
			"id": 423,
			"path": "\/29-pipelining-1_djvu.xml",
			"bytes": 164407,
			"selected": 1
		},
		{
			"id": 424,
			"path": "\/29-pipelining-1_hocr.html",
			"bytes": 360430,
			"selected": 1
		},
		{
			"id": 425,
			"path": "\/29-pipelining-1_hocr_pageindex.json.gz",
			"bytes": 170,
			"selected": 1
		},
		{
			"id": 426,
			"path": "\/29-pipelining-1_hocr_searchtext.txt.gz",
			"bytes": 3950,
			"selected": 1
		},
		{
			"id": 427,
			"path": "\/29-pipelining-1_jp2.zip",
			"bytes": 3357295,
			"selected": 1
		},
		{
			"id": 428,
			"path": "\/29-pipelining-1_page_numbers.json",
			"bytes": 2673,
			"selected": 1
		},
		{
			"id": 429,
			"path": "\/29-pipelining-1_scandata.xml",
			"bytes": 4548,
			"selected": 1
		},
		{
			"id": 430,
			"path": "\/29-pipelining-1_spectrogram.png",
			"bytes": 280249,
			"selected": 1
		},
		{
			"id": 431,
			"path": "\/30-pipelining-2.mp3",
			"bytes": 12368039,
			"selected": 1
		},
		{
			"id": 432,
			"path": "\/30-pipelining-2.pdf",
			"bytes": 309503,
			"selected": 1
		},
		{
			"id": 433,
			"path": "\/30-pipelining-2.png",
			"bytes": 61399,
			"selected": 1
		},
		{
			"id": 434,
			"path": "\/30-pipelining-2_chocr.html.gz",
			"bytes": 139743,
			"selected": 1
		},
		{
			"id": 435,
			"path": "\/30-pipelining-2_djvu.txt",
			"bytes": 9902,
			"selected": 1
		},
		{
			"id": 436,
			"path": "\/30-pipelining-2_djvu.xml",
			"bytes": 158281,
			"selected": 1
		},
		{
			"id": 437,
			"path": "\/30-pipelining-2_hocr.html",
			"bytes": 351352,
			"selected": 1
		},
		{
			"id": 438,
			"path": "\/30-pipelining-2_hocr_pageindex.json.gz",
			"bytes": 180,
			"selected": 1
		},
		{
			"id": 439,
			"path": "\/30-pipelining-2_hocr_searchtext.txt.gz",
			"bytes": 3367,
			"selected": 1
		},
		{
			"id": 440,
			"path": "\/30-pipelining-2_jp2.zip",
			"bytes": 3549921,
			"selected": 1
		},
		{
			"id": 441,
			"path": "\/30-pipelining-2_page_numbers.json",
			"bytes": 2819,
			"selected": 1
		},
		{
			"id": 442,
			"path": "\/30-pipelining-2_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 443,
			"path": "\/30-pipelining-2_spectrogram.png",
			"bytes": 273258,
			"selected": 1
		},
		{
			"id": 444,
			"path": "\/31-caches-1.mp3",
			"bytes": 11820095,
			"selected": 1
		},
		{
			"id": 445,
			"path": "\/31-caches-1.pdf",
			"bytes": 249321,
			"selected": 1
		},
		{
			"id": 446,
			"path": "\/31-caches-1.png",
			"bytes": 62019,
			"selected": 1
		},
		{
			"id": 447,
			"path": "\/31-caches-1_chocr.html.gz",
			"bytes": 200698,
			"selected": 1
		},
		{
			"id": 448,
			"path": "\/31-caches-1_djvu.txt",
			"bytes": 14470,
			"selected": 1
		},
		{
			"id": 449,
			"path": "\/31-caches-1_djvu.xml",
			"bytes": 228537,
			"selected": 1
		},
		{
			"id": 450,
			"path": "\/31-caches-1_hocr.html",
			"bytes": 505693,
			"selected": 1
		},
		{
			"id": 451,
			"path": "\/31-caches-1_hocr_pageindex.json.gz",
			"bytes": 273,
			"selected": 1
		},
		{
			"id": 452,
			"path": "\/31-caches-1_hocr_searchtext.txt.gz",
			"bytes": 4788,
			"selected": 1
		},
		{
			"id": 453,
			"path": "\/31-caches-1_jp2.zip",
			"bytes": 5326451,
			"selected": 1
		},
		{
			"id": 454,
			"path": "\/31-caches-1_page_numbers.json",
			"bytes": 4851,
			"selected": 1
		},
		{
			"id": 455,
			"path": "\/31-caches-1_scandata.xml",
			"bytes": 7616,
			"selected": 1
		},
		{
			"id": 456,
			"path": "\/31-caches-1_spectrogram.png",
			"bytes": 273867,
			"selected": 1
		},
		{
			"id": 457,
			"path": "\/32-caches-2.mp3",
			"bytes": 11924062,
			"selected": 1
		},
		{
			"id": 458,
			"path": "\/32-caches-2.pdf",
			"bytes": 197261,
			"selected": 1
		},
		{
			"id": 459,
			"path": "\/32-caches-2.png",
			"bytes": 64334,
			"selected": 1
		},
		{
			"id": 460,
			"path": "\/32-caches-2_chocr.html.gz",
			"bytes": 154848,
			"selected": 1
		},
		{
			"id": 461,
			"path": "\/32-caches-2_djvu.txt",
			"bytes": 11088,
			"selected": 1
		},
		{
			"id": 462,
			"path": "\/32-caches-2_djvu.xml",
			"bytes": 183801,
			"selected": 1
		},
		{
			"id": 463,
			"path": "\/32-caches-2_hocr.html",
			"bytes": 411904,
			"selected": 1
		},
		{
			"id": 464,
			"path": "\/32-caches-2_hocr_pageindex.json.gz",
			"bytes": 169,
			"selected": 1
		},
		{
			"id": 465,
			"path": "\/32-caches-2_hocr_searchtext.txt.gz",
			"bytes": 4077,
			"selected": 1
		},
		{
			"id": 466,
			"path": "\/32-caches-2_jp2.zip",
			"bytes": 3063200,
			"selected": 1
		},
		{
			"id": 467,
			"path": "\/32-caches-2_page_numbers.json",
			"bytes": 2535,
			"selected": 1
		},
		{
			"id": 468,
			"path": "\/32-caches-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 469,
			"path": "\/32-caches-2_spectrogram.png",
			"bytes": 271774,
			"selected": 1
		},
		{
			"id": 470,
			"path": "\/33-caches-3.mp3",
			"bytes": 12312137,
			"selected": 1
		},
		{
			"id": 471,
			"path": "\/33-caches-3.pdf",
			"bytes": 2152544,
			"selected": 1
		},
		{
			"id": 472,
			"path": "\/33-caches-3.png",
			"bytes": 43673,
			"selected": 1
		},
		{
			"id": 473,
			"path": "\/33-caches-3_chocr.html.gz",
			"bytes": 146720,
			"selected": 1
		},
		{
			"id": 474,
			"path": "\/33-caches-3_djvu.txt",
			"bytes": 10421,
			"selected": 1
		},
		{
			"id": 475,
			"path": "\/33-caches-3_djvu.xml",
			"bytes": 170977,
			"selected": 1
		},
		{
			"id": 476,
			"path": "\/33-caches-3_hocr.html",
			"bytes": 374426,
			"selected": 1
		},
		{
			"id": 477,
			"path": "\/33-caches-3_hocr_pageindex.json.gz",
			"bytes": 178,
			"selected": 1
		},
		{
			"id": 478,
			"path": "\/33-caches-3_hocr_searchtext.txt.gz",
			"bytes": 4009,
			"selected": 1
		},
		{
			"id": 479,
			"path": "\/33-caches-3_jp2.zip",
			"bytes": 3187263,
			"selected": 1
		},
		{
			"id": 480,
			"path": "\/33-caches-3_page_numbers.json",
			"bytes": 2760,
			"selected": 1
		},
		{
			"id": 481,
			"path": "\/33-caches-3_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 482,
			"path": "\/33-caches-3_spectrogram.png",
			"bytes": 280474,
			"selected": 1
		},
		{
			"id": 483,
			"path": "\/34-virtual-memory-1.mp3",
			"bytes": 12080169,
			"selected": 1
		},
		{
			"id": 484,
			"path": "\/34-virtual-memory-1.pdf",
			"bytes": 181002,
			"selected": 1
		},
		{
			"id": 485,
			"path": "\/34-virtual-memory-1.png",
			"bytes": 46222,
			"selected": 1
		},
		{
			"id": 486,
			"path": "\/34-virtual-memory-1_chocr.html.gz",
			"bytes": 133454,
			"selected": 1
		},
		{
			"id": 487,
			"path": "\/34-virtual-memory-1_djvu.txt",
			"bytes": 9517,
			"selected": 1
		},
		{
			"id": 488,
			"path": "\/34-virtual-memory-1_djvu.xml",
			"bytes": 144906,
			"selected": 1
		},
		{
			"id": 489,
			"path": "\/34-virtual-memory-1_hocr.html",
			"bytes": 312738,
			"selected": 1
		},
		{
			"id": 490,
			"path": "\/34-virtual-memory-1_hocr_pageindex.json.gz",
			"bytes": 160,
			"selected": 1
		},
		{
			"id": 491,
			"path": "\/34-virtual-memory-1_hocr_searchtext.txt.gz",
			"bytes": 3530,
			"selected": 1
		},
		{
			"id": 492,
			"path": "\/34-virtual-memory-1_jp2.zip",
			"bytes": 3141795,
			"selected": 1
		},
		{
			"id": 493,
			"path": "\/34-virtual-memory-1_page_numbers.json",
			"bytes": 2398,
			"selected": 1
		},
		{
			"id": 494,
			"path": "\/34-virtual-memory-1_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 495,
			"path": "\/34-virtual-memory-1_spectrogram.png",
			"bytes": 279355,
			"selected": 1
		},
		{
			"id": 496,
			"path": "\/35-virtual-memory-2.mp3",
			"bytes": 11900134,
			"selected": 1
		},
		{
			"id": 497,
			"path": "\/35-virtual-memory-2.pdf",
			"bytes": 230145,
			"selected": 1
		},
		{
			"id": 498,
			"path": "\/35-virtual-memory-2.png",
			"bytes": 46198,
			"selected": 1
		},
		{
			"id": 499,
			"path": "\/35-virtual-memory-2_chocr.html.gz",
			"bytes": 156498,
			"selected": 1
		},
		{
			"id": 500,
			"path": "\/35-virtual-memory-2_djvu.txt",
			"bytes": 11204,
			"selected": 1
		},
		{
			"id": 501,
			"path": "\/35-virtual-memory-2_djvu.xml",
			"bytes": 182223,
			"selected": 1
		},
		{
			"id": 502,
			"path": "\/35-virtual-memory-2_hocr.html",
			"bytes": 418467,
			"selected": 1
		},
		{
			"id": 503,
			"path": "\/35-virtual-memory-2_hocr_pageindex.json.gz",
			"bytes": 169,
			"selected": 1
		},
		{
			"id": 504,
			"path": "\/35-virtual-memory-2_hocr_searchtext.txt.gz",
			"bytes": 3935,
			"selected": 1
		},
		{
			"id": 505,
			"path": "\/35-virtual-memory-2_jp2.zip",
			"bytes": 3529460,
			"selected": 1
		},
		{
			"id": 506,
			"path": "\/35-virtual-memory-2_page_numbers.json",
			"bytes": 2640,
			"selected": 1
		},
		{
			"id": 507,
			"path": "\/35-virtual-memory-2_scandata.xml",
			"bytes": 4416,
			"selected": 1
		},
		{
			"id": 508,
			"path": "\/35-virtual-memory-2_spectrogram.png",
			"bytes": 281408,
			"selected": 1
		},
		{
			"id": 509,
			"path": "\/36-io.mp3",
			"bytes": 12020088,
			"selected": 1
		},
		{
			"id": 510,
			"path": "\/36-io.pdf",
			"bytes": 299907,
			"selected": 1
		},
		{
			"id": 511,
			"path": "\/36-io.png",
			"bytes": 45659,
			"selected": 1
		},
		{
			"id": 512,
			"path": "\/36-io_chocr.html.gz",
			"bytes": 208058,
			"selected": 1
		},
		{
			"id": 513,
			"path": "\/36-io_djvu.txt",
			"bytes": 14952,
			"selected": 1
		},
		{
			"id": 514,
			"path": "\/36-io_djvu.xml",
			"bytes": 225463,
			"selected": 1
		},
		{
			"id": 515,
			"path": "\/36-io_hocr.html",
			"bytes": 496618,
			"selected": 1
		},
		{
			"id": 516,
			"path": "\/36-io_hocr_pageindex.json.gz",
			"bytes": 204,
			"selected": 1
		},
		{
			"id": 517,
			"path": "\/36-io_hocr_searchtext.txt.gz",
			"bytes": 5459,
			"selected": 1
		},
		{
			"id": 518,
			"path": "\/36-io_jp2.zip",
			"bytes": 4369980,
			"selected": 1
		},
		{
			"id": 519,
			"path": "\/36-io_page_numbers.json",
			"bytes": 3317,
			"selected": 1
		},
		{
			"id": 520,
			"path": "\/36-io_scandata.xml",
			"bytes": 5376,
			"selected": 1
		},
		{
			"id": 521,
			"path": "\/36-io_spectrogram.png",
			"bytes": 283738,
			"selected": 1
		},
		{
			"id": 522,
			"path": "\/37-networks.mp3",
			"bytes": 12332095,
			"selected": 1
		},
		{
			"id": 523,
			"path": "\/37-networks.pdf",
			"bytes": 776128,
			"selected": 1
		},
		{
			"id": 524,
			"path": "\/37-networks.png",
			"bytes": 46149,
			"selected": 1
		},
		{
			"id": 525,
			"path": "\/37-networks_chocr.html.gz",
			"bytes": 227408,
			"selected": 1
		},
		{
			"id": 526,
			"path": "\/37-networks_djvu.txt",
			"bytes": 16965,
			"selected": 1
		},
		{
			"id": 527,
			"path": "\/37-networks_djvu.xml",
			"bytes": 241728,
			"selected": 1
		},
		{
			"id": 528,
			"path": "\/37-networks_hocr.html",
			"bytes": 509775,
			"selected": 1
		},
		{
			"id": 529,
			"path": "\/37-networks_hocr_pageindex.json.gz",
			"bytes": 165,
			"selected": 1
		},
		{
			"id": 530,
			"path": "\/37-networks_hocr_searchtext.txt.gz",
			"bytes": 6881,
			"selected": 1
		},
		{
			"id": 531,
			"path": "\/37-networks_jp2.zip",
			"bytes": 3111734,
			"selected": 1
		},
		{
			"id": 532,
			"path": "\/37-networks_page_numbers.json",
			"bytes": 2449,
			"selected": 1
		},
		{
			"id": 533,
			"path": "\/37-networks_scandata.xml",
			"bytes": 4096,
			"selected": 1
		},
		{
			"id": 534,
			"path": "\/37-networks_spectrogram.png",
			"bytes": 278315,
			"selected": 1
		},
		{
			"id": 535,
			"path": "\/38-disks.mp3",
			"bytes": 11984039,
			"selected": 1
		},
		{
			"id": 536,
			"path": "\/38-disks.pdf",
			"bytes": 6304288,
			"selected": 1
		},
		{
			"id": 537,
			"path": "\/38-disks.png",
			"bytes": 46571,
			"selected": 1
		},
		{
			"id": 538,
			"path": "\/38-disks_chocr.html.gz",
			"bytes": 185381,
			"selected": 1
		},
		{
			"id": 539,
			"path": "\/38-disks_djvu.txt",
			"bytes": 13177,
			"selected": 1
		},
		{
			"id": 540,
			"path": "\/38-disks_djvu.xml",
			"bytes": 208399,
			"selected": 1
		},
		{
			"id": 541,
			"path": "\/38-disks_hocr.html",
			"bytes": 453998,
			"selected": 1
		},
		{
			"id": 542,
			"path": "\/38-disks_hocr_pageindex.json.gz",
			"bytes": 191,
			"selected": 1
		},
		{
			"id": 543,
			"path": "\/38-disks_hocr_searchtext.txt.gz",
			"bytes": 5188,
			"selected": 1
		},
		{
			"id": 544,
			"path": "\/38-disks_jp2.zip",
			"bytes": 3995258,
			"selected": 1
		},
		{
			"id": 545,
			"path": "\/38-disks_page_numbers.json",
			"bytes": 3113,
			"selected": 1
		},
		{
			"id": 546,
			"path": "\/38-disks_scandata.xml",
			"bytes": 5056,
			"selected": 1
		},
		{
			"id": 547,
			"path": "\/38-disks_spectrogram.png",
			"bytes": 275955,
			"selected": 1
		},
		{
			"id": 548,
			"path": "\/39-performance-1.mp3",
			"bytes": 12140042,
			"selected": 1
		},
		{
			"id": 549,
			"path": "\/39-performance-1.png",
			"bytes": 47261,
			"selected": 1
		},
		{
			"id": 550,
			"path": "\/39-performance-1_spectrogram.png",
			"bytes": 281600,
			"selected": 1
		},
		{
			"id": 551,
			"path": "\/39-performance-2.mp3",
			"bytes": 11912046,
			"selected": 1
		},
		{
			"id": 552,
			"path": "\/39-performance-2.png",
			"bytes": 47209,
			"selected": 1
		},
		{
			"id": 553,
			"path": "\/39-performance-2_spectrogram.png",
			"bytes": 278590,
			"selected": 1
		},
		{
			"id": 554,
			"path": "\/39-performance.pdf",
			"bytes": 316804,
			"selected": 1
		},
		{
			"id": 555,
			"path": "\/39-performance_chocr.html.gz",
			"bytes": 186813,
			"selected": 1
		},
		{
			"id": 556,
			"path": "\/39-performance_djvu.txt",
			"bytes": 13635,
			"selected": 1
		},
		{
			"id": 557,
			"path": "\/39-performance_djvu.xml",
			"bytes": 191756,
			"selected": 1
		},
		{
			"id": 558,
			"path": "\/39-performance_hocr.html",
			"bytes": 398028,
			"selected": 1
		},
		{
			"id": 559,
			"path": "\/39-performance_hocr_pageindex.json.gz",
			"bytes": 183,
			"selected": 1
		},
		{
			"id": 560,
			"path": "\/39-performance_hocr_searchtext.txt.gz",
			"bytes": 5112,
			"selected": 1
		},
		{
			"id": 561,
			"path": "\/39-performance_jp2.zip",
			"bytes": 3869180,
			"selected": 1
		},
		{
			"id": 562,
			"path": "\/39-performance_page_numbers.json",
			"bytes": 2872,
			"selected": 1
		},
		{
			"id": 563,
			"path": "\/39-performance_scandata.xml",
			"bytes": 4736,
			"selected": 1
		},
		{
			"id": 564,
			"path": "\/40-x86.mp3",
			"bytes": 8760111,
			"selected": 1
		},
		{
			"id": 565,
			"path": "\/40-x86.pdf",
			"bytes": 248008,
			"selected": 1
		},
		{
			"id": 566,
			"path": "\/40-x86.png",
			"bytes": 47230,
			"selected": 1
		},
		{
			"id": 567,
			"path": "\/40-x86_chocr.html.gz",
			"bytes": 249946,
			"selected": 1
		},
		{
			"id": 568,
			"path": "\/40-x86_djvu.txt",
			"bytes": 17991,
			"selected": 1
		},
		{
			"id": 569,
			"path": "\/40-x86_djvu.xml",
			"bytes": 265571,
			"selected": 1
		},
		{
			"id": 570,
			"path": "\/40-x86_hocr.html",
			"bytes": 572490,
			"selected": 1
		},
		{
			"id": 571,
			"path": "\/40-x86_hocr_pageindex.json.gz",
			"bytes": 224,
			"selected": 1
		},
		{
			"id": 572,
			"path": "\/40-x86_hocr_searchtext.txt.gz",
			"bytes": 6862,
			"selected": 1
		},
		{
			"id": 573,
			"path": "\/40-x86_jp2.zip",
			"bytes": 4479043,
			"selected": 1
		},
		{
			"id": 574,
			"path": "\/40-x86_page_numbers.json",
			"bytes": 3602,
			"selected": 1
		},
		{
			"id": 575,
			"path": "\/40-x86_scandata.xml",
			"bytes": 6016,
			"selected": 1
		},
		{
			"id": 576,
			"path": "\/40-x86_spectrogram.png",
			"bytes": 279919,
			"selected": 1
		},
		{
			"id": 577,
			"path": "\/41-introduction-to-reconfigurable-computing.mp3",
			"bytes": 12072124,
			"selected": 1
		},
		{
			"id": 578,
			"path": "\/41-introduction-to-reconfigurable-computing.png",
			"bytes": 48058,
			"selected": 1
		},
		{
			"id": 579,
			"path": "\/41-introduction-to-reconfigurable-computing.ppt",
			"bytes": 4370432,
			"selected": 1
		},
		{
			"id": 580,
			"path": "\/41-introduction-to-reconfigurable-computing_spectrogram.png",
			"bytes": 269602,
			"selected": 1
		},
		{
			"id": 581,
			"path": "\/42-class-summary.mp3",
			"bytes": 10680111,
			"selected": 1
		},
		{
			"id": 582,
			"path": "\/42-class-summary.png",
			"bytes": 46060,
			"selected": 1
		},
		{
			"id": 583,
			"path": "\/42-class-summary_spectrogram.png",
			"bytes": 282868,
			"selected": 1
		},
		{
			"id": 584,
			"path": "\/README.txt",
			"bytes": 62,
			"selected": 1
		},
		{
			"id": 585,
			"path": "\/__ia_thumb.jpg",
			"bytes": 3823,
			"selected": 1
		},
		{
			"id": 586,
			"path": "\/uc-berkeley-cs61c-great-ideas-in-computer-architecture_meta.sqlite",
			"bytes": 278528,
			"selected": 1
		},
		{
			"id": 587,
			"path": "\/uc-berkeley-cs61c-great-ideas-in-computer-architecture_meta.xml",
			"bytes": 1342,
			"selected": 1
		},
		{
			"id": 588,
			"path": "\/mars\/mars.jar",
			"bytes": 4169142,
			"selected": 1
		},
		{
			"id": 589,
			"path": "\/mars\/mips.asm",
			"bytes": 145,
			"selected": 1
		}
	],
	"links": [
	],
	"speed": 1141000,
	"seeders": 4
}
        """
                .trimIndent()
        val item: TorrentItem = jsonAdapter.fromJson(json)!!

        val torrentStructure: Node<TorrentFileItem> = getFilesNodes(item, false)

        var newValue = false
        Node.traverseNodeDepthFirst(torrentStructure) {
            if (it.value.id == 2) {
                newValue = !it.value.selected
                it.value.selected = newValue
            }
        }
        Node.traverseDepthFirst(torrentStructure) {
            if (it.id == 2) {
                assert(it.selected == newValue)
            }
        }
    }
}
