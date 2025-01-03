![image](https://github.com/user-attachments/assets/6a992469-b2ce-4a51-bf53-56601cfa474b)

## 주요 설명
- **NFC기능을 활용한 출입관리 앱**: 현재 출결,출석 어플들은 다른 핸드폰으로 아이디, 비밀번호만 알면 로그인이 가능하다. 이런 보안적으로 문제가 있는것을 보완해 출석 및 출입관리 앱을 만들자.
- **수상 경력 및 설명**: 
  - 총 세명이 참가한 프로젝트로 핵심 멤버는 본인(김민석),이희우
  - 성결대학교 전 학과가 참여하는 교내 핵심 "창의문제해결"프로젝트로 우수상(2등 상금 90만원) 수상  

- **작업 기간**: 2024.9-11월
- **시연 영상**: 
  - (https://blog.naver.com/everybetter/223687785732)

---
## 기술 스택
프로그래밍 언어: Kotlin (Android Studio) <br>
데이터베이스: Firebase Realtime Database,Firebase Storge<br>
NFC 기술: Android NFC API

## 주요 기능
- **NFC 인증 기반 출입관리**  
    NFC 태그를 스캔하여 출입 확인
  - **유심 기반 로그인 기능**
  -  핸드폰 기기의 “유심칩”에서 번호를 뽑아 로그인시 1차로 자신의 핸드폰 유심과 입력한 번호가 맞는지 대조후,<br>
      2차로 관리자가 등록해둔 사용자의 핸드폰 번호와 자신이 입력한 번호와 비교해, 등록되어있다면 다음 화면으로 넘어가 출석을 할 수 있게 구성을 했다.<br>
      즉 타인의 핸드폰에서 절대 로그인 할 수 없는 기능이다. 본인의 핸드폰에서만 로그인이 가능하다.<br>
       이 기능은 현재 출시된 어플들에는 존재하지 않는 기능으로 완벽하게 대리출석을 방지할 수 있다.
 - **리더기 모드**  
  핸드폰 자체를 NFC태그를 읽는 리더기 모드로 구성(0번 누르면 작동)
- **사용자 관리 시스템**  
  관리자(교수,인사담당자 등)/일반 유저(학생,회사원)으로 구분된 회원가입을 제공해 앱 내에서 사용자 정보를 등록, 삭제, 수정 가능.
- **출입 내역 조회**  
  자신이 출입이 되었는지 확인가능
- **실시간 알림**  
  관리자(교수,인사담당자 등)/일반 유저(학생,회사원) 으로 나뉘어진 부분에서 양쪽 메시지 기능.
- **데이터 AES알고리즘을 통한 암호화/ 복호화 기능을 통해 본인 정보 확인**  
  DB에 저장되는 데이터들 전부 암호화되어 유출시 안전.
  사용자(본인)에게는 복호화를 통해 암호화된 데이터를 가져와 정보 확인가능.
 - **다국어 기능 지원**  

---


## 개발자 소개
- **개발자**: 김민석,이희우
- **사용 언어**:Kotlin
- **출시 플랫폼**: Android (구글 플레이스토어)

---
## 화면 구성

<table>
  <tr>
    <td>
      <h3>로그인 화면</h3>
      <img src="https://github.com/user-attachments/assets/4e8dfa23-1138-4fe5-a42d-50dc79430b39" alt="로그인 화면" width="300">
    </td>
    <td>
      <h3>메인 화면 및 출석 화면</h3>
      <img src="https://github.com/user-attachments/assets/be448288-6ee2-4d3e-8ff3-76d315a24198" alt="메인 화면 및 출석 화면" width="300">
    </td>
  </tr>
  <tr>
    <td>
      <h3>리더기 모드</h3>
      <img src="https://github.com/user-attachments/assets/e5af7725-ed3c-4df8-8aa2-6b112d8e2eb8" alt="리더기 모드" width="300">
    </td>
    <td>
      <h3>쪽지, 지각 이의신청, 휴가신청</h3>
      <img src="https://github.com/user-attachments/assets/d2bc94ce-e788-4a0d-a66d-bb6d656be73a" alt="쪽지, 지각 이의신청, 휴가신청" width="300">
    </td>
  </tr>
  <tr>
    <td>
      <h3>출장관리, 모바일 신분증 화면</h3>
      <img src="https://github.com/user-attachments/assets/bd3fc455-ee37-4c27-a2c5-1d6b2dabf972" alt="출장관리, 모바일 신분증 화면" width="300">
    </td>
    <td>
      <h3>다국어 지원 기능</h3>
      <img src="https://github.com/user-attachments/assets/1b5fc1d1-939a-468e-96e4-7f54c622009a" alt="다국어 지원 기능" width="300">
    </td>
  </tr>
  <tr>
    <td>
      <h3>핸드폰 유심과의 비교</h3>
      <img src="https://github.com/user-attachments/assets/4fcad854-424b-497c-94ad-ab9a1a21eb79" alt="핸드폰 유심과의 비교" width="300">
    </td>
    <td>
      <h3>유저모드 (학생, 사원)</h3>
      <img src="https://github.com/user-attachments/assets/d4f94c4c-041e-40e8-8dda-5a5d0904c53e" alt="유저모드" width="300">
    </td>
  </tr>
</table>
