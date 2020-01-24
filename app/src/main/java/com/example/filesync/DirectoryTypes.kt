package com.example.filesync

enum class DirectoryTypes (val type: String) {
    SOFT_ROOT("/"), MUSIC("Music"), VIDEO("Videos"), IMAGE("Images"), DOCUMENTS("Documents");
    companion object {
        fun getByName(name: String) = when (name) {
            "/" -> SOFT_ROOT
            "Music" -> MUSIC
            "Videos" -> VIDEO
            "Images" -> IMAGE
            else -> DOCUMENTS
        }
    }
}