#!/bin/bash
# Final-2team-DrHong 환경변수 설정 스크립트
# 사용법: source .env.sh

export JWT_SECRET_KEY="bRmQUS6ug6iZPbR3BCzfSzGByCH2xtZwBZNGZ2jC0Tp1FssGET7Lwkp6XmgBSdTo7IfxCXtwAsE7Wu1UH5oeYg=="
export DB_PASSWORD="drhong1!"
export MAIL_PASSWORD="svaieothjdtxtwyo"

echo "✅ 환경변수 설정 완료:"
echo "   - JWT_SECRET_KEY: 설정됨"
echo "   - DB_PASSWORD: 설정됨"  
echo "   - MAIL_PASSWORD: 설정됨"
echo ""
echo "📝 사용 방법:"
echo "   source .env.sh && ./gradlew bootRun    # 애플리케이션 실행"
echo "   source .env.sh && ./gradlew test       # 테스트 실행"