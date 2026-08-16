(function () {
    var map, marker, geocoder;

    // appkey 미설정 또는 카카오 개발자 콘솔에 현재 접속 도메인이 플랫폼(Web)으로
    // 등록되어 있지 않으면 SDK 스크립트가 window.kakao를 정의하지 못한 채 끝난다 —
    // 그 상태로 kakao.maps.load()를 호출하면 전체 폼 스크립트가 죽으므로 방어한다.
    var kakaoAvailable = typeof kakao !== 'undefined' && !!kakao.maps;
    if (!kakaoAvailable) {
        console.error('[Kakao Maps] SDK 로드 실패 — appkey 또는 카카오 개발자 콘솔의 Web 플랫폼 도메인 등록을 확인하세요.');
        var mapEl = document.getElementById('kakaoMap');
        if (mapEl) mapEl.textContent = '지도를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.';
    } else {
        kakao.maps.load(function () {
            var container = document.getElementById('kakaoMap');
            var options = {
                center: new kakao.maps.LatLng(37.5665, 126.9780),
                level: 5
            };
            map = new kakao.maps.Map(container, options);
            marker = new kakao.maps.Marker({ position: options.center, map: null });
            geocoder = new kakao.maps.services.Geocoder();

            kakao.maps.event.addListener(map, 'click', function (mouseEvent) {
                var latlng = mouseEvent.latLng;
                placeMarker(latlng.getLat(), latlng.getLng());
                geocoder.coord2Address(latlng.getLng(), latlng.getLat(), function (result, status) {
                    if (status === kakao.maps.services.Status.OK) {
                        var addr = result[0].road_address
                            ? result[0].road_address.address_name
                            : result[0].address.address_name;
                        document.getElementById('locationInput').value = addr;
                    }
                });
            });
        });
    }

    function placeMarker(lat, lng) {
        var pos = new kakao.maps.LatLng(lat, lng);
        marker.setPosition(pos);
        marker.setMap(map);
        map.setCenter(pos);
        document.getElementById('latInput').value = lat;
        document.getElementById('lngInput').value = lng;
    }

    // keyword로 장소/주소를 찾아 지도에 좌표를 찍는다.
    // updateLocationField가 true면 검색으로 찾은 정식 명칭으로 "장소" 필드도 덮어쓴다
    // (주소 검색창 사용 시) — false면 좌표만 채우고 사용자가 쓴 원문은 그대로 둔다
    // ("장소" 필드에서 자동 보완할 때).
    function geocodeKeyword(keyword, updateLocationField) {
        if (!kakaoAvailable || !keyword) return;
        var ps = new kakao.maps.services.Places();
        var addrErr = document.getElementById('addr-search-error');
        ps.keywordSearch(keyword, function (data, status) {
            if (status === kakao.maps.services.Status.OK) {
                var place = data[0];
                placeMarker(parseFloat(place.y), parseFloat(place.x));
                if (updateLocationField) document.getElementById('locationInput').value = place.place_name;
                if (addrErr) addrErr.style.display = 'none';
            } else {
                geocoder.addressSearch(keyword, function (result, status) {
                    if (status === kakao.maps.services.Status.OK) {
                        placeMarker(parseFloat(result[0].y), parseFloat(result[0].x));
                        if (updateLocationField) document.getElementById('locationInput').value = result[0].address_name;
                        if (addrErr) addrErr.style.display = 'none';
                    } else if (updateLocationField && addrErr) {
                        // "장소" 필드 자동 보완 실패는 조용히 넘어간다 — 자유 텍스트라
                        // 검색 실패가 흔한데 매번 에러를 띄우면 오히려 방해된다
                        addrErr.style.display = 'block';
                    }
                });
            }
        });
    }

    function searchAddress() {
        var keyword = document.getElementById('addrSearch').value.trim();
        if (keyword) geocodeKeyword(keyword, true);
    }

    document.getElementById('addrSearch').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); searchAddress(); }
    });

    document.getElementById('addr-search-btn').addEventListener('click', searchAddress);

    // 장소 필드에 직접 입력한 텍스트로도 자동으로 좌표를 채운다 — 이미 지도
    // 클릭/주소 검색으로 좌표가 채워져 있으면 덮어쓰지 않는다
    var locationInput = document.getElementById('locationInput');
    if (locationInput) {
        locationInput.addEventListener('blur', function () {
            if (document.getElementById('latInput').value) return;
            geocodeKeyword(this.value.trim(), false);
        });
    }

    function filterArtists(query) {
        var q = query.toLowerCase();
        document.querySelectorAll('#artistList .artist-item').forEach(function (item) {
            item.style.display = item.dataset.name.includes(q) || (item.dataset.nameEn || '').includes(q) || (item.dataset.aliases || '').includes(q) ? '' : 'none';
        });
    }

    var startDateInput = document.getElementById('startDate');
    var endDateInput   = document.getElementById('endDate');
    if (startDateInput && endDateInput) {
        startDateInput.addEventListener('change', function () {
            var val = this.value;
            endDateInput.min = val;
            if (!endDateInput.value || endDateInput.value < val) {
                endDateInput.value = val;
            }
        });
    }

    var artistSearchInput = document.getElementById('artistSearch');
    if (artistSearchInput) {
        artistSearchInput.addEventListener('input', function () {
            filterArtists(this.value);
        });
    }

    document.querySelectorAll('[required]').forEach(function (field) {
        field.addEventListener('invalid', function () {
            var err = this.parentElement.querySelector('.field-error');
            if (err) err.style.display = 'block';
            this.style.borderColor = 'var(--danger)';
        });
        field.addEventListener('input', function () {
            if (this.value.trim()) {
                var err = this.parentElement.querySelector('.field-error');
                if (err) err.style.display = 'none';
                this.style.borderColor = '';
            }
        });
        field.addEventListener('change', function () {
            if (this.value) {
                var err = this.parentElement.querySelector('.field-error');
                if (err) err.style.display = 'none';
                this.style.borderColor = '';
            }
        });
    });
})();
