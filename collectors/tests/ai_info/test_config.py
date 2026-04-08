"""test_config.py"""
from pathlib import Path
from collectors.scripts.ai_info import config
from collectors.scripts.shared import config as shared_config

def test_ai_info_config() -> None:
    assert isinstance(config.ROOT_DIR, Path)
    assert isinstance(config.DATA_DIR, Path)
    assert isinstance(config.AI_INFO_DIR, Path)
    assert isinstance(config.OPENROUTER_MODELS_URL, str)
    assert isinstance(config.ARTIFICIAL_ANALYSIS_URL, str)
    assert isinstance(config.OCI_PREFIX, str)
    assert isinstance(config.OCI_UPLOAD_TARGETS, list)

    for local_path, object_name in config.OCI_UPLOAD_TARGETS:
        assert isinstance(local_path, Path)
        assert isinstance(object_name, str)
        assert str(local_path).find("ai-info") != -1
        assert object_name.startswith(config.OCI_PREFIX)

def test_shared_config_exists() -> None:
    """shared/config.py 설정값 존재 확인"""
    assert isinstance(shared_config.REQUEST_TIMEOUT_SECONDS, int)
    assert isinstance(shared_config.MAX_RETRY_ATTEMPTS, int)
    assert isinstance(shared_config.RETRY_WAIT_MIN_SECONDS, int)
    assert isinstance(shared_config.RETRY_WAIT_MAX_SECONDS, int)