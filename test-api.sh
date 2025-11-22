#!/bin/bash

# API 테스트 스크립트

echo "=== DM API 테스트 시작 ==="

BASE_URL="http://localhost:8080"

echo "1. 스레드 목록 조회 (빈 목록 확인)"
curl -X GET "${BASE_URL}/messages/threads?currentUserPid=user_me" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n\n2. 새로운 스레드 생성"
curl -X POST "${BASE_URL}/messages/threads" \
  -H "Content-Type: application/json" \
  -d '{
    "currentUserPid": "user_me",
    "targetPid": "user_21"
  }' | jq '.'

echo -e "\n\n3. 스레드 목록 조회 (생성된 스레드 확인)"
curl -X GET "${BASE_URL}/messages/threads?currentUserPid=user_me" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n\n4. 메시지 전송"
curl -X POST "${BASE_URL}/messages/threads/1/messages" \
  -H "Content-Type: application/json" \
  -d '{
    "currentUserPid": "user_me",
    "content": "안녕하세요! 첫 번째 메시지입니다."
  }' | jq '.'

echo -e "\n\n5. 메시지 조회"
curl -X GET "${BASE_URL}/messages/threads/1?currentUserPid=user_me" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n\n6. 스레드 목록 재조회 (마지막 메시지 포함)"
curl -X GET "${BASE_URL}/messages/threads?currentUserPid=user_me" \
  -H "Content-Type: application/json" | jq '.'

echo -e "\n\n=== API 테스트 완료 ==="