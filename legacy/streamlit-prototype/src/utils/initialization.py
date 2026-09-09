import streamlit as st
from src.app.config import OpenAIConfig

def initialize_chatbot_service():
    """ChatbotService 초기화"""
    from src.core.services.chatbot_service import ChatbotService
    return ChatbotService(OpenAIConfig())