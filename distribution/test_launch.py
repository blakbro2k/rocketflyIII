import unittest
from unittest.mock import MagicMock, patch

import launch


class StartupTests(unittest.TestCase):
    @patch("launch.time.sleep")
    def test_reset_then_ready(self, sleep):
        with patch("launch.urlopen", side_effect=[ConnectionResetError(104, "reset"), MagicMock()]) as probe:
            launch.wait_for_startup("http://127.0.0.1:18080")
        self.assertEqual(probe.call_count, 2)
        sleep.assert_called_once()

    def test_persistent_reset_reports_diagnostics(self):
        with patch("launch.time.monotonic", side_effect=[0, 61]), patch(
            "launch.urlopen", side_effect=ConnectionResetError(104, "reset")
        ):
            with self.assertRaisesRegex(RuntimeError, "Last error: .*reset.*docker logs"):
                launch.wait_for_startup("http://127.0.0.1:18080")
