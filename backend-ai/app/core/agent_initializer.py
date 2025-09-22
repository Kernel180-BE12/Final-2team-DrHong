import os
from dotenv import load_dotenv

from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate
from langchain_google_genai import ChatGoogleGenerativeAI

class AgentInitializer:
    agent_executor: AgentExecutor = None

    @classmethod
    def initialize(cls):
        load_dotenv()
        GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")

        llm = ChatGoogleGenerativeAI(model='gemini-2.5-flash')

        if os.getenv('IS_GPU_AVAILABLE') == 'TRUE': # agent 가 파인튜닝 된 템플릿 생성 모델을 사용해서 Template 생성
            from ..services.tools import generate_template, parse_template_from_generative_model_output, create_final_output
            tools = [generate_template, parse_template_from_generative_model_output, create_final_output]
            prompt = ChatPromptTemplate.from_messages(
                messages=[
                    ("system",
                     """
                     You are a methodical AI agent specializing in generating and modifying Kakao Alert Talk templates.
                     Your goal is to reliably produce a valid template that meets the user's request and complies with all policies.

                     You have access to the following tools:
                     - `generate_template`: Use this to call a fine-tuned model to create a raw template string based on the inputs.
                     - `parse_template_from_generative_model_output`: Use this to parse and validate the raw string from the model into a structured template.
                     - `create_final_output`: This is your final step. Use this to format the final, validated template and a friendly chat message for the user.

                     Always follow this sequence:
                     1.  Carefully analyze the original template, user request, and policies.
                     2.  Use the `generate_template` tool to create a first draft.
                     3.  Take the raw output string and use the `parse_template_from_generative_model_output` tool to validate it.

                     **CRITICAL INSTRUCTION: ERROR HANDLING**
                     - If a tool returns an error message in the 'Observation' (e.g., "Parsing failed"), you MUST analyze the error and try to fix the problem in your next thought process.
                     - If `parse_template_from_generative_model_output` fails because the JSON is malformed, you MUST call `generate_template` again, but this time, be more careful to generate a perfectly structured JSON string.
                     - Your task is complete only when you have successfully called the `create_final_output` tool with a validated template.
                     """),
                    ("human", "Original Template:\n{original_template}\n\nUser Request: {user_input}\n\nRelated Policies: {related_policy}\n"),
                    ("placeholder", "{agent_scratchpad}"),
                ]
            )
        else: # agent 가 직접 Template 생성
            from ..services.tools import create_final_output
            tools = [create_final_output]
            prompt = ChatPromptTemplate.from_messages(
                messages=[
                    ("system",
                     """
                     You are a highly precise template modification assistant. Your sole purpose is to accurately populate or refine a given template based on user instructions.
            
                     ### --- Template Syntax --- ###
                     - The template has three fields: "title", "text", and "button_name".
                     - You can include variables using the format #{{변수명}}.
            
                     ### --- CORE DIRECTIVE --- ###
                     Your goal is to take an `Original Template` and apply a `User Request` to produce a `Modified Template`. You MUST follow these steps in order.
            
                     **Step 1: Analyze the 'Original Template' Input.**
                     - Look at the values of the "title", "text", and "button_name" fields.
            
                     **Step 2: Execute Your Task Based on Step 1.**
                     - **IF a field in the `Original Template` is an empty string ("")**: Your task is to **POPULATE** that field with content based on the `User Request`.
                     - **IF a field already has content**: Your task is to **REFINE** that content based on the `User Request`.
            
                     ### --- UNBREAKABLE RULES --- ###
                     1.  **PRESERVE UNCHANGED FIELDS**: This is your most important rule. When refining, you MUST carry over the exact content of any fields that were not part of the user's modification request. Do not leave them empty.
                         -   **Example**: If the `Original Template` is `{{"title": "A", "text": "B"}}` and the `User Request` is "Change the title to C", your final result MUST be `{{"title": "C", "text": "B"}}`. The 'text' field is preserved.
            
                     2.  **ALWAYS START WITH THE `Original Template`**. It is your canvas. Never ignore it or start from a blank slate.
                     3.  **STICK TO THE TOPIC**. The context provided in the original template is absolute. Do not change the subject.
            
                     After processing the template, you MUST use the `create_final_output` tool to format the response. All responses MUST be in Korean.
                     """),
                    ("human", "Original Template:\n{original_template}\n\nUser Request: {user_input}\n\nRelated Policies: {related_policy}\n"),
                    ("placeholder", "{agent_scratchpad}"),
                ]
            )

        cls.agent_executor = AgentExecutor(
            agent = create_tool_calling_agent(llm=llm.bind_tools(tools=tools), tools=tools, prompt=prompt),
            tools = tools,
            return_intermediate_steps=True,
            verbose = True,
            handle_parsing_errors=True
        )

        return cls.agent_executor
