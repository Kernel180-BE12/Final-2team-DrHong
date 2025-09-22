import re
import json
import ast
from langchain_core.tools import tool, ToolException

from typing import Annotated, List
from pydantic import ValidationError

from ..core.model_loader import model_loader
from ..schemas.template import Template, AgentOutput

@tool
def create_final_output(
        template: Annotated[
            Template,
            "A fully-formed and validated Pydantic Template object to be returned as the final output."
        ],
        chat_message: Annotated[
            str,
            "A brief, friendly, and conversational message to the user, summarizing the result."
        ]
) -> AgentOutput:
    """
    Combines a validated Template object and a conversational message into the final structured `AgentOutput`.
    This tool should be called as the final step in the agent's process to produce the complete and formatted response.
    """
    return AgentOutput(
        template=template,
        chat_message=chat_message
    )


@tool
def parse_template_from_generative_model_output(
    model_output: Annotated[
        str, "The raw output from a generative model that contains a JSON string, potentially with other text."
    ]
) -> Template:
    """
    Parses the first complete JSON object from a raw model output string and validates it against the Template Pydantic class.
    """
    try:
        # 첫 번째 여는 중괄호 '{'를 찾습니다.
        start_idx = model_output.find('{')
        if start_idx == -1:
            raise ValueError("Could not find start of JSON object '{' in model output.")

        # 여는 중괄호와 닫는 중괄호의 개수를 세어 짝이 맞는 첫 번째 JSON 객체의 끝을 찾습니다.
        open_braces = 1
        end_idx = -1
        for i in range(start_idx + 1, len(model_output)):
            char = model_output[i]
            if char == '{':
                open_braces += 1
            elif char == '}':
                open_braces -= 1

            if open_braces == 0:
                end_idx = i
                break

        if end_idx == -1:
            raise ValueError("Could not find matching closing brace '}' for JSON object.")

        # 정확히 첫 번째 JSON 객체 문자열만 추출합니다.
        template_str = model_output[start_idx : end_idx + 1]

        # ast.literal_eval로 파이썬 딕셔너리로 변환 후 Pydantic으로 검증합니다.
        template_dict = ast.literal_eval(template_str)
        return Template.model_validate(template_dict)

    except (ValidationError, ValueError, SyntaxError) as e:
        raise ToolException(f"Could not parse string into Template Pydantic class: {e}")
    except Exception as e:
        raise ToolException(f"An unexpected error occurred during parsing: {e}")

@tool
def generate_template(
        original_template: Annotated[
            Template,
            ""
        ],
        user_input: Annotated[
            str,
            "The client's specific request or query to be used as a prompt for the template generation. "
            "This should be the main content provided by the user."
        ],
        related_policy: Annotated[
            List[str],
            "A list of specific policy documents or rules relevant to the user's request. "
            "This is essential for the model to generate an accurate and compliant template. "
            "The list can contain multiple strings, where each string is a different policy."
        ]
) -> str:
    """
    Generates a structured template in JSON format based on a user's request and a list of specific policies.
    This tool is designed to be called when a user needs a new template created according to predefined rules or constraints.
    The output is a raw string containing the generated JSON template.
    """
    import torch

    models = model_loader.models
    gen_model, gen_tokenizer = models.get("gen")

    one_shot_example = """user_input: 회사소개서 발송 템플릿 제작 부탁드려요
policy: ["정보성 메시지란 정보통신망법 안내서에 '영리목적 광고성 정보의 예외'에 해당하는 메시지입니다."]
template: {"title": "회사소개서 발송", "text": "안녕하세요 #{수신자명}님,\\n\\n#{회사명}은 #{업종} 분야에서 활동하는 #{회사명}입니다.\\n\\n▶ 회사명 : #{회사명}\\n▶ 업종 : #{업종}\\n▶ 연락처 : #{연락처}\\n\\n감사합니다.", "button_name": "자세히 보기"}"""

    actual_request = f"""user_input: {user_input}
policy: {related_policy}
template:"""

    prompt = f"{one_shot_example}\n\n---\n\n{actual_request}"

    inputs = gen_tokenizer(prompt, return_tensors="pt", max_length=512, truncation=True).to(gen_model.device)

    with torch.no_grad():
        outputs = gen_model.generate(
            **inputs,
            max_new_tokens=150,
            temperature=0.1,
            do_sample=True,
            pad_token_id=gen_tokenizer.eos_token_id
        )

    full_response = gen_tokenizer.decode(outputs[0], skip_special_tokens=True)

    # 'template:' 뒤의 내용만 반환하도록 더 안정적으로 수정
    if 'template:' in full_response:
        return full_response.split('template:')[-1].strip()
    else:
        return full_response
