from dotenv import load_dotenv
import os

# 환경변수 로드 (파일이 없어도 계속 진행)
try:
    load_dotenv(raise_error_if_not_found=False)
except Exception as e:
    print(f"Warning: Could not load .env file: {str(e)}")

# SSL 검증 비활성화
os.environ['CURL_CA_BUNDLE'] = ''

# 환경변수 확인
print("\n=== Global Environment Variables ===")
print(f"PINECONE_API_KEY: {os.getenv('PINECONE_API_KEY')}")
print(f"PINECONE_ENVIRONMENT: {os.getenv('PINECONE_ENVIRONMENT')}")
print(f"PINECONE_INDEX_NAME: {os.getenv('PINECONE_INDEX_NAME')}")
print("=== End Global Environment Variables ===\n") 