from setuptools import setup, find_packages

try:
    with open("requirements.txt") as f:
        requirements = [
            line.strip()
            for line in f
            if line.strip() and not line.startswith("--") and not line.startswith("#")
        ]
except FileNotFoundError:
    requirements = []
    print("⚠️  requirements.txt 파일을 찾을 수 없습니다. 설치 종속성이 누락될 수 있습니다.")

setup(
    name="mini-project-2",
    version="0.1.0",
    description="음성·감정인식 기반 수면상담 RAG 챗봇",
    author="Your Name",
    author_email="your@email.com",
    packages=find_packages(),
    install_requires=requirements,
    python_requires=">=3.8",
)
