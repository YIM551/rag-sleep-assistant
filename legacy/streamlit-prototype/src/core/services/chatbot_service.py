import os
from dataclasses import dataclass
from typing import Dict, Optional, Tuple, List, Any
from transformers import pipeline
from langchain_openai import ChatOpenAI
from src.app.constants import DEFAULT_PERSONA, PERSONA_PROMPTS
from src.utils.error_handling import handle_streamlit_errors
from src.utils.rag_utils import RAGUtils

@dataclass
class ChatbotService:
    def __init__(self, openai_config):
        self.llm = ChatOpenAI(
            api_key=openai_config.api_key,
            model_name=openai_config.chat_model,
            temperature=openai_config.temperature
        )
        self._initialize_emotion_classifier()
        
        # RAG 초기화 시도
        try:
            self.rag_utils = RAGUtils()
            self.rag_enabled = True
            print("RAG functionality enabled successfully")
        except Exception as e:
            print(f"\n=== RAG Initialization Failed ===")
            print(f"Error: {str(e)}")
            print("Falling back to basic chat mode without RAG")
            print("To enable RAG, please check your Pinecone settings")
            print("=== End RAG Error ===\n")
            self.rag_enabled = False

    def _initialize_emotion_classifier(self):
        """감정 분석 모델 초기화"""
        os.environ["TOKENIZERS_PARALLELISM"] = "false"
        self.emotion_classifier = pipeline(
            "text-classification", 
            model="j-hartmann/emotion-english-distilroberta-base",
            top_k=None,
            device=-1
        )

    @handle_streamlit_errors
    def analyze_emotion(self, text: str) -> Dict[str, float]:
        """텍스트의 감정을 분석하여 감정별 점수를 반환"""
        results = self.emotion_classifier(text)
        return {score['label']: score['score'] for score in results[0]}

    @handle_streamlit_errors
    def get_response(self, user_input: str, persona_name: str) -> Tuple[str, List[Dict[str, Any]]]:
        print(f"\n=== Generating Response ===")
        print(f"Persona: {persona_name}")
        print(f"RAG Enabled: {self.rag_enabled}")
        
        persona_prompt = PERSONA_PROMPTS.get(persona_name, PERSONA_PROMPTS[DEFAULT_PERSONA])
        
        try:
            if self.rag_enabled:
                print("Attempting to retrieve relevant context...")
                augmented_prompt, reference_docs = self.rag_utils.get_augmented_prompt(user_input, persona_name)
                print(f"Retrieved {len(reference_docs)} relevant documents")
            else:
                print("RAG is disabled, using basic chat mode")
                augmented_prompt = ""
                reference_docs = []
        except Exception as e:
            print(f"Error during RAG retrieval: {str(e)}")
            print("Falling back to basic chat mode for this response")
            augmented_prompt = ""
            reference_docs = []
        
        prompt = f"""
        {persona_prompt}
        
        {augmented_prompt}
        
        사용자 메시지: {user_input}
        
        위 페르소나 설정을 바탕으로 사용자의 메시지에 공감하고 적절한 응답을 해주세요.
        응답은 반드시 한국어로 해주세요.
        응답에는 페르소나의 특징이 잘 드러나야 합니다.
        응답에는 '페르소나의 메시지:', '챗봇의 메시지:' 등의 접두어를 포함하지 마세요.
        """

        response = self.llm.invoke(prompt)
        return response.content.strip(), reference_docs

