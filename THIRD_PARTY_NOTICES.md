# Third-party notices

## AndroidX Media3 FFmpeg decoder extension

FortressTV includes the AndroidX Media3 FFmpeg decoder extension from Media3
1.10.1, commit `5fb306449733dd71595700c1227ad6087578c559`.

AndroidX Media3 is licensed under the Apache License 2.0.

Source: https://github.com/androidx/media/tree/1.10.1

## FFmpeg

This software uses code of FFmpeg licensed under the GNU Lesser General Public
License version 2.1 or later. FortressTV's bundled FFmpeg build is based on
FFmpeg 6.0 commit `3f92512fd1fd6f5e6d6eb45a156c352835314d69` and enables only the
`mp3` decoder used by Media3 for MPEG Layer I, II and III audio.

The exact corresponding source archive, licence, build configuration and
rebuild instructions are provided in `third_party/ffmpeg`.

Upstream project: https://ffmpeg.org/

FortressTV does not enable FFmpeg's GPL or non-free components.
