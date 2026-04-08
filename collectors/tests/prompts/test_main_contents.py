"""test_main_contents.py"""
import pytest
from unittest.mock import patch, MagicMock
from pathlib import Path

from collectors.scripts.prompts.main_contents import _cleanup

@patch("collectors.scripts.prompts.main_contents.WORK_DIR")
@patch("collectors.scripts.prompts.main_contents.shutil.rmtree")
def test_cleanup(mock_rmtree: MagicMock, mock_work_dir: MagicMock) -> None:
    mock_work_dir.exists.return_value = True
    _cleanup()
    mock_rmtree.assert_called_once_with(mock_work_dir)