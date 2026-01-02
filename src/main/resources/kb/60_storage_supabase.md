# Supabase Storage(이미지 업로드)

이 프로젝트는 Supabase Storage를 사용해 이미지 업로드를 처리합니다.

## 설정 값
prefix: `supabase.*`
- `supabase.url`
- `supabase.service-role-key`
- `supabase.storage.bucket`
- `supabase.storage.public-bucket` (true면 public URL 반환)

## 업로드 공통 규칙
- 파일이 비어있으면 400
- 이미지 타입만 허용(jpg/png/webp)
- private bucket이면 업로드는 되더라도 public URL을 즉시 만들 수 없어서 `null`을 반환(현재 signed URL 미구현)

## 사용되는 API
- 프로필 업로드: `POST /api/user/profile/image`
- 채팅 이미지 업로드: `POST /api/chats/rooms/{roomId}/images`
- 채팅방 대표 이미지: `POST /api/chats/rooms`(multipart)
