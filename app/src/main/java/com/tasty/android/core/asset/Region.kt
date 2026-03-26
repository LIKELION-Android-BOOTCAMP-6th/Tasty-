package com.tasty.android.core.asset

data class SubRegion(
    val name: String
)

data class MainRegion(
    val name: String,
    val subRegions: List<SubRegion>
)

object RegionData {

    val mainRegions = listOf(

        MainRegion(
            name = "서울",
            subRegions = listOf(
                SubRegion("강남구"),
                SubRegion("강동구"),
                SubRegion("강북구"),
                SubRegion("강서구"),
                SubRegion("관악구"),
                SubRegion("광진구"),
                SubRegion("구로구"),
                SubRegion("금천구"),
                SubRegion("노원구"),
                SubRegion("도봉구"),
                SubRegion("동대문구"),
                SubRegion("동작구"),
                SubRegion("마포구"),
                SubRegion("서대문구"),
                SubRegion("서초구"),
                SubRegion("성동구"),
                SubRegion("성북구"),
                SubRegion("송파구"),
                SubRegion("양천구"),
                SubRegion("영등포구"),
                SubRegion("용산구"),
                SubRegion("은평구"),
                SubRegion("종로구"),
                SubRegion("중구"),
                SubRegion("중랑구")
            )
        ),

        MainRegion(
            name = "경기",
            subRegions = listOf(
                SubRegion("가평군"),
                SubRegion("고양시 덕양구"),
                SubRegion("고양시 일산동구"),
                SubRegion("고양시 일산서구"),
                SubRegion("과천시"),
                SubRegion("광명시"),
                SubRegion("광주시"),
                SubRegion("구리시"),
                SubRegion("군포시"),
                SubRegion("김포시"),
                SubRegion("남양주시"),
                SubRegion("동두천시"),
                SubRegion("부천시 소사구"),
                SubRegion("부천시 오정구"),
                SubRegion("부천시 원미구"),
                SubRegion("성남시 분당구"),
                SubRegion("성남시 수정구"),
                SubRegion("성남시 중원구"),
                SubRegion("수원시 권선구"),
                SubRegion("수원시 영통구"),
                SubRegion("수원시 장안구"),
                SubRegion("수원시 팔달구"),
                SubRegion("시흥시"),
                SubRegion("안산시 단원구"),
                SubRegion("안산시 상록구"),
                SubRegion("안성시"),
                SubRegion("안양시 동안구"),
                SubRegion("안양시 만안구"),
                SubRegion("양주시"),
                SubRegion("양평군"),
                SubRegion("여주시"),
                SubRegion("연천군"),
                SubRegion("오산시"),
                SubRegion("용인시 기흥구"),
                SubRegion("용인시 수지구"),
                SubRegion("용인시 처인구"),
                SubRegion("의왕시"),
                SubRegion("의정부시"),
                SubRegion("이천시"),
                SubRegion("파주시"),
                SubRegion("평택시"),
                SubRegion("포천시"),
                SubRegion("하남시"),
                SubRegion("화성시")
            )
        ),

        MainRegion(
            name = "인천",
            subRegions = listOf(
                SubRegion("강화군"),
                SubRegion("계양구"),
                SubRegion("남동구"),
                SubRegion("동구"),
                SubRegion("미추홀구"),
                SubRegion("부평구"),
                SubRegion("서구"),
                SubRegion("연수구"),
                SubRegion("옹진군"),
                SubRegion("중구")
            )
        ),

        MainRegion(
            name = "부산",
            subRegions = listOf(
                SubRegion("강서구"),
                SubRegion("금정구"),
                SubRegion("기장군"),
                SubRegion("남구"),
                SubRegion("동구"),
                SubRegion("동래구"),
                SubRegion("부산진구"),
                SubRegion("북구"),
                SubRegion("사상구"),
                SubRegion("사하구"),
                SubRegion("서구"),
                SubRegion("수영구"),
                SubRegion("연제구"),
                SubRegion("영도구"),
                SubRegion("중구"),
                SubRegion("해운대구")
            )
        ),

        MainRegion(
            name = "대구",
            subRegions = listOf(
                SubRegion("군위군"),
                SubRegion("남구"),
                SubRegion("달서구"),
                SubRegion("달성군"),
                SubRegion("동구"),
                SubRegion("북구"),
                SubRegion("서구"),
                SubRegion("수성구"),
                SubRegion("중구")
            )
        ),

        MainRegion(
            name = "대전",
            subRegions = listOf(
                SubRegion("대덕구"),
                SubRegion("동구"),
                SubRegion("서구"),
                SubRegion("유성구"),
                SubRegion("중구")
            )
        ),

        MainRegion(
            name = "광주",
            subRegions = listOf(
                SubRegion("광산구"),
                SubRegion("남구"),
                SubRegion("동구"),
                SubRegion("북구"),
                SubRegion("서구")
            )
        ),

        MainRegion(
            name = "울산",
            subRegions = listOf(
                SubRegion("남구"),
                SubRegion("동구"),
                SubRegion("북구"),
                SubRegion("울주군"),
                SubRegion("중구")
            )
        ),

        MainRegion(
            name = "경남",
            subRegions = listOf(
                SubRegion("거제시"),
                SubRegion("거창군"),
                SubRegion("고성군"),
                SubRegion("김해시"),
                SubRegion("남해군"),
                SubRegion("밀양시"),
                SubRegion("사천시"),
                SubRegion("산청군"),
                SubRegion("양산시"),
                SubRegion("의령군"),
                SubRegion("진주시"),
                SubRegion("창녕군"),
                SubRegion("창원시 마산합포구"),
                SubRegion("창원시 마산회원구"),
                SubRegion("창원시 성산구"),
                SubRegion("창원시 의창구"),
                SubRegion("창원시 진해구"),
                SubRegion("통영시"),
                SubRegion("하동군"),
                SubRegion("함안군"),
                SubRegion("함양군"),
                SubRegion("합천군")
            )
        ),

        MainRegion(
            name = "경북",
            subRegions = listOf(
                SubRegion("경산시"),
                SubRegion("경주시"),
                SubRegion("고령군"),
                SubRegion("구미시"),
                SubRegion("김천시"),
                SubRegion("문경시"),
                SubRegion("봉화군"),
                SubRegion("상주시"),
                SubRegion("성주군"),
                SubRegion("안동시"),
                SubRegion("영덕군"),
                SubRegion("영양군"),
                SubRegion("영주시"),
                SubRegion("영천시"),
                SubRegion("예천군"),
                SubRegion("울릉군"),
                SubRegion("울진군"),
                SubRegion("의성군"),
                SubRegion("청도군"),
                SubRegion("청송군"),
                SubRegion("칠곡군"),
                SubRegion("포항시 남구"),
                SubRegion("포항시 북구")
            )
        ),

        MainRegion(
            name = "강원",
            subRegions = listOf(
                SubRegion("강릉시"),
                SubRegion("고성군"),
                SubRegion("동해시"),
                SubRegion("삼척시"),
                SubRegion("속초시"),
                SubRegion("양구군"),
                SubRegion("양양군"),
                SubRegion("영월군"),
                SubRegion("원주시"),
                SubRegion("인제군"),
                SubRegion("정선군"),
                SubRegion("철원군"),
                SubRegion("춘천시"),
                SubRegion("태백시"),
                SubRegion("평창군"),
                SubRegion("홍천군"),
                SubRegion("화천군"),
                SubRegion("횡성군")
            )
        ),

        MainRegion(
            name = "충북",
            subRegions = listOf(
                SubRegion("괴산군"),
                SubRegion("단양군"),
                SubRegion("보은군"),
                SubRegion("영동군"),
                SubRegion("옥천군"),
                SubRegion("음성군"),
                SubRegion("제천시"),
                SubRegion("증평군"),
                SubRegion("진천군"),
                SubRegion("청주시 상당구"),
                SubRegion("청주시 서원구"),
                SubRegion("청주시 청원구"),
                SubRegion("청주시 흥덕구"),
                SubRegion("충주시")
            )
        ),

        MainRegion(
            name = "충남",
            subRegions = listOf(
                SubRegion("계룡시"),
                SubRegion("공주시"),
                SubRegion("금산군"),
                SubRegion("논산시"),
                SubRegion("당진시"),
                SubRegion("보령시"),
                SubRegion("부여군"),
                SubRegion("서산시"),
                SubRegion("서천군"),
                SubRegion("아산시"),
                SubRegion("예산군"),
                SubRegion("천안시 동남구"),
                SubRegion("천안시 서북구"),
                SubRegion("청양군"),
                SubRegion("태안군"),
                SubRegion("홍성군")
            )
        ),

        MainRegion(
            name = "전남",
            subRegions = listOf(
                SubRegion("강진군"),
                SubRegion("고흥군"),
                SubRegion("곡성군"),
                SubRegion("광양시"),
                SubRegion("구례군"),
                SubRegion("나주시"),
                SubRegion("담양군"),
                SubRegion("목포시"),
                SubRegion("무안군"),
                SubRegion("보성군"),
                SubRegion("순천시"),
                SubRegion("신안군"),
                SubRegion("여수시"),
                SubRegion("영광군"),
                SubRegion("완도군"),
                SubRegion("장성군"),
                SubRegion("장흥군"),
                SubRegion("진도군"),
                SubRegion("함평군"),
                SubRegion("해남군"),
                SubRegion("화순군")
            )
        ),

        MainRegion(
            name = "전북",
            subRegions = listOf(
                SubRegion("고창군"),
                SubRegion("군산시"),
                SubRegion("김제시"),
                SubRegion("남원시"),
                SubRegion("무주군"),
                SubRegion("부안군"),
                SubRegion("순창군"),
                SubRegion("완주군"),
                SubRegion("익산시"),
                SubRegion("임실군"),
                SubRegion("장수군"),
                SubRegion("전주시 덕진구"),
                SubRegion("전주시 완산구"),
                SubRegion("정읍시"),
                SubRegion("진안군")
            )
        ),

        MainRegion(
            name = "제주",
            subRegions = listOf(
                SubRegion("서귀포시"),
                SubRegion("제주시")
            )
        ),

        MainRegion(
            name = "세종",
            subRegions = listOf(
                SubRegion("세종시")
            )
        )
    )
}